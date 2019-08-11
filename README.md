## 事件溯源

#### 1. 问题的提出：

在传统的CRUD数据建模方式中，涉及到聚合（也就是领域模型）的变更之时，只会保存其最新的状态，这样会造成一系列问题：

* 所有历史状态都丢失，无法在出问题时回溯到以前的状态。
* 在涉及到共享状态的变更时往往会用到锁，降低了吞吐效率。
* 分布式环境下，微服务各自保存自己的状态，但要在跨服务之间做到保证事务一致性时，没有很好的解决方案。

#### 2. 问题的解决：

事件溯源是将数据库写操作建模为事件流的一种模式。这与CRUD模型中将聚合快照存储在数据库中不同，它把所有更改作为事件流存储在聚合中。我们有这个聚合的所有操作序列，它包含了从初始到当前的所有状态。

通过这些信息，可以回滚到过去任何时间节点的聚合快照，并再次返回到当前快照。这为整个系统提供了完整的可追溯性，可以对它们进行审计或调试。

这可以想象成是一盒磁带，我们可以通过录音机来回放过去录下来的音乐，甚至可以快退，快进到任何时间点。

#### 3. CQRS

CQRS称为命令查询责任分离，这种模式是一种鼓励单独领域模型各自处理命令和查询的模式。他在领域模型及其持久化中提供了读写分离。

在这种模式下，有一个写模型保证了应用状态的一致性，而读模型可以有多个，从而应对各种各样的查询需求。

一般来说，事件溯源和CQRS可以结合使用。本例也是将这两个模型结合使用:

![image](https://github.com/kennykong/eventSourcing/raw/master/images/eventSourcing-CQRS.png)

#### 4. 模型建立
典型的模型就是个人银行的领域模型。有一些核心的行为：在银行开立账户，将钱存入银行，从账户中取款，以及查看余额。其中，前三个涉及到改变模型的状态，而查看余额是一个读操作。要实现这些行为，将使用受以下操作影响的Account聚合。

* 开设一个新账户(open)：在内存中创建一个新聚合并更新底层持久化状态。
* 将钱存入现有账户(credit)：更新现有账户聚合的余额。
* 从现有账户提款(debit)：更新现有账户的余额，或在余额不足的情况下抛出异常。
* 检查余额(balance)：从账户中返回余额，无应用程序状态更改
* 转账(transform)：从账户A转账一笔钱到账户B

模型的定义：
* 业务领域对象有：Account，Balance
* 命令有：Open，Close，Credit，Debit
* 事件有：Opened，Closed，Credited，Debited
* 抽象的，作为辅助功能实现的对象有：Event, Command, Aggregate，Snapshot

他们之间的关系：
* Aggregate是作为业务领域对象 Account 的基类，Balance是Account持有的属性
* Event作为4种事件的基类
* Command作为4种命令的基类
* Snapshot是各种状态的Aggregate的序列

类图：
![image](https://github.com/kennykong/eventSourcing/raw/master/images/ES-model.png)


#### 5. 事件溯源中的命令和事件
命令是改变模型状态的请求，而事件是这个状态改变已经发生了，一旦发生就具有不可变性，因而可以作为真相的来源。命令和事件的交互过程如下：
* 用户执行了一个命令（例如：开设账户或进行一个交易）
* 该命令创建一个新的聚合，或者更新现有聚合的状态
* 无论哪种情况，它都构建了一个聚合，进行了一些领域验证，并执行了一些可能包括副作用的操作。如果一切顺利，它会生成一个事件并将其添加到写模型中。注意我们并不更新写模型中的任何东西，只是添加新的事件。它本质上是一个按顺序增长的事件流。
* 在事件日志中添加记录可以为感兴趣的订阅者创建通知。他们订阅这些事件并更新他们的读模型。

事件的特征：
* 通用语言：事件的名称必须是领域词汇，有自己的命名空间，而且由于事件都是已发生的，其名称都必须是过去时态。比如我们可以定义账户开立这个事件名称为AccountOpened。
* 触发：命令或其他事件的处理都可触发一个事件。
* 发布-订阅：兴趣方可以订阅特定的事件流，并更新自己的读模型。
* 不可变：事件不能被更新，只能新建并添加。
* 时间戳：任何事件都有自己的发生时间，所以必须包含一个时间戳。


#### 6. 用 Free Monad 管理副作用

Free Monad本质上就是为了解耦：将领域行为建模为*纯数据*，并提供*解释程序*，它将在特定上下文中对这些数据展开工作。这与编译器的工作方式类似：将程序转化为一个抽象语法树（AST）,他是一个纯数据，然后定义独立的解释程序来操作树并执行各自的转化，比如说打印或者变成一个异步线程。

本例中，我们将Command抽象成是和Event相关的monand，把Open, Close, Debit, Credit 等定义为代数数据类型（ADT），然后就可以通过moand组合的力量（scala的 for...yeild 语句），建立起一个个组合操作，这种组合操作也可以称作*DSL（领域特定语言）*，如：转账，开户并向账户里存入一笔钱等等。
最后建立起一个解释器，把各个基本操作转化为异步线程Task，完成有副作用的操作。由于各个基本操作都是异步执行，程序自然而然的具有一定的*响应性*。

#### 7. 怎样跑起来
下载源代码到本地，然后通过intelliJ导入源代码，构建形式选择sbt，构建完成后在app包下面有个“run.sc”，打开，然后看到左上角有个绿色运行箭头，按下即可执行。
或者右键选择“Evaluate worksheet”。
![image](https://github.com/kennykong/eventSourcing/raw/master/images/run.png)
