package com.example.go_channel

import com.example.lua_coroutinu.utils.log
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

class Go {

}
/*
这是一个用kotlin模仿go语言的Channel
实现效果：
fun plainChannelSample() {
    val channel = SimpleChannel<Int>()

    go("producer"){
        for (i in 0..6) {
            log("send", i)
            channel.send(i)
        }
    }

    go("consumer", channel::close){
        for (i in 0..5) {
            log("receive")
            val got = channel.receive()
            log("got", got)
        }
    }
}
fun main() {
    plainChannelSample()
}
*/
interface Channel<T>{
    suspend fun send(value:T)
    suspend fun receive():T
    fun close()
}
class SimpleChannel<T> :Channel<T>{
    sealed class Element{
        object None:Element()    //None代表既没有生产者也没有消费者
        class Producer<T>(val value:T,val continuation:Continuation<Unit>): Element()
        class Consume<T>(val continuation: Continuation<T>): Element()
        object Closed: Element()
    }
    //传入的是Element.None是因为要确保开始时候通道明确状态，既没有生产者也没有消费者
    private val status = AtomicReference<Element>(Element.None)

    override suspend fun send(value: T)= suspendCoroutine<Unit> {continuation->
        val prev = status.getAndUpdate {
            when(it){
                //Element.None表示既没有生产者也没有消费者，在这种情况下消费者会变成Element.Producer
                //Element.Consumer<*>表示有一个等待生产者的消费者，后面设置为None，是因为生产任务已经完成
                Element.Closed -> throw IllegalStateException("Cannot send after closed.")
                is Element.Consume<*> -> Element.None
                is Element.Producer<*> -> throw IllegalStateException("Cannot send new element while previous is not consumed.")
                Element.None -> Element.Producer(value,continuation)
            }
        }
        (prev as? Element.Consume<T>)?.continuation?.resume(value).let { continuation.resume(Unit) }
    }

    override suspend fun receive(): T = suspendCoroutine<T>{continuation->
        val prev = status.getAndUpdate{
            when(it){
                Element.Closed -> throw IllegalStateException("Cannot receive new element after closed.")
                is Element.Consume<*> -> throw IllegalStateException("Cannot receive new element while previous is not provided.")
                is Element.Producer<*> -> Element.None
                Element.None -> Element.Consume(continuation)
            }
        }
        (prev as? Element.Producer<T>)?.let {
            it.continuation.resume(Unit)
            continuation.resume(it.value)
        }
    }

    override fun close() {
        val prev = status.getAndUpdate { Element.Closed }

        if (prev is Element.Consume<*>) {
            prev.continuation.resumeWithException(ClosedException("Channel is closed."))
        } else if (prev is Element.Producer<*>) {
            prev.continuation.resumeWithException(ClosedException("Channel is closed."))
        }
    }
}
class ClosedException(message:String):Exception(message)


fun plainChannelSample() {
    val channel = SimpleChannel<Int>()

    go("producer"){
        for (i in 0..6) {
            log("send", i)
            channel.send(i)
        }
    }

    go("consumer", channel::close){
        for (i in 0..5) {
            log("receive")
            val got = channel.receive()
            log("got", got)
        }
    }
}
fun go(name: String = "", completion: () -> Unit = {}, block: suspend () -> Unit){
    block.startCoroutine(object : Continuation<Any> {
        override val context = DispatcherContext()

        override fun resumeWith(result: Result<Any>) {
            log("end $name", result)
            completion()
        }
    })
}

fun main() {
    plainChannelSample()

}