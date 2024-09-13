# Go 风格的 Channel 实现

这是一个用 Kotlin 模仿 Go 语言中 Channel 概念的实现。通过协程与通道实现生产者和消费者模式，在 Kotlin 中实现了类似 Go 语言中 goroutine 和 Channel 的功能。

## 功能描述

本项目通过使用 Kotlin 的协程机制，模仿了 Go 语言中 `goroutine` 和 `Channel` 的特性，提供了生产者和消费者之间的数据流机制。项目的核心部分是 `SimpleChannel` 类，用于实现生产者和消费者的非阻塞通信。

### 特性

- **生产者和消费者模式**：通过 `go` 函数启动协程，实现生产者向通道发送数据，消费者从通道接收数据。
- **通道关闭机制**：支持通道的关闭，并在关闭时给正在等待的生产者或消费者抛出异常。
- **线程安全**：使用 `AtomicReference` 实现状态管理，保证通道的线程安全。

## 使用方法

### 示例代码

#### 生产者和消费者

```kotlin
fun plainChannelSample() {
    val channel = SimpleChannel<Int>()

    go("producer") {
        for (i in 0..6) {
            log("send", i)
            channel.send(i)
        }
    }

    go("consumer", channel::close) {
        for (i in 0..5) {
            log("receive")
            val got = channel.receive()
            log("got", got)
        }
    }
}
```
### 核心类

#### `Channel<T>`

通道接口，定义了 `send`、`receive` 和 `close` 方法，用于处理协程间的通信。

#### `SimpleChannel<T>`

`SimpleChannel` 类是核心实现，包含以下特性：

- **生产者和消费者的状态流转**：通过内部的 `Element` 类来记录通道当前的状态（无生产者、无消费者、生产者等待、消费者等待等）。
- **`send` 和 `receive` 的挂起与恢复机制**：使用 `suspendCoroutine` 来处理生产者与消费者的挂起和恢复操作。
- **通道关闭处理**：通过 `close` 方法关闭通道，并在关闭时抛出 `ClosedException`。

#### `go` 函数

```kotlin
fun go(name: String = "", completion: () -> Unit = {}, block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Any> {
        override val context = DispatcherContext()
        override fun resumeWith(result: Result<Any>) {
            log("end $name", result)
            completion()
        }
    })
}
```
## 如何运行

1. 确保已经安装 JDK 1.8 以上版本，并配置好 Kotlin 开发环境。

2. 克隆此项目到本地：

    ```bash
    git clone https://github.com/你的用户名/你的项目名.git
    ```

3. 在项目根目录下，使用 Gradle 构建项目：

    ```bash
    ./gradlew build
    ```

4. 运行 `main` 函数，观察生产者和消费者之间的数据交互。

## 示例输出

```bash
send 0
receive
got 0
send 1
receive
got 1
send 2
receive
got 2
send 3
receive
got 3
send 4
receive
got 4
send 5
receive
got 5
```
