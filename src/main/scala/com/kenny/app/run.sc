import com.kenny.interpreter.RepositoryBackedAccountInterpreter
import com.kenny.interpreter.RepositoryBackedAccountInterpreter._
import com.kenny.service.Scripts._

//free monad 解释 composite 操作
RepositoryBackedAccountInterpreter(composite)
// 运行得到结果
res0.unsafePerformSync

//新建操作
for {
  a <- credit("a-123", 1000)
  b <- open("a-125", "john jack", Some(org.joda.time.DateTime.now()))
  c <- credit(b.no, 1200)
} yield c

//解释上一个操作
RepositoryBackedAccountInterpreter(res2)
//得到结果
res3.unsafePerformSync

//查看账户a-123的事件集
eventLog.events("a-123")
//查看账户a-125的事件集
eventLog.events("a-125")

import com.kenny.service.AccountSnapshot._
import scalaz.Scalaz._
import scalaz._

//合并上两个事件集
res5 |+| res6

//折叠运行事件集，并获取两个账户最新快照
res7 map snapshot

//查看所有事件集
eventLog.allEvents

//折叠运行所有事件集，并获取所有个账户最新快照
res9 map snapshot

//free monad 解释 compositeFail 操作, 会抛余额不足异常
RepositoryBackedAccountInterpreter(compositeFail)

//执行操作，抛余额不足异常
res11.unsafePerformSync

//查看账户a-124的事件集
eventLog.events("a-124")

//建立转账操作，a-123 向 a-124 转账 300元
transfer("a-123", "a-124", 300)

//解释操作
RepositoryBackedAccountInterpreter(res14)
//执行操作
res15.unsafePerformSync

//查看账户a-124的所有事件集
eventLog.events("a-124")
//查看账户a-123的所有事件集
eventLog.events("a-123")

//建立转账操作，a-123 向 a-124 转账 60000元，实际余额不足
transfer("a-123", "a-124", 60000)

//解释操作
RepositoryBackedAccountInterpreter(res19)

//执行操作，抛出余额不足异常
res20.unsafePerformSync

//账户a-123的最新状态
balance("a-123")

