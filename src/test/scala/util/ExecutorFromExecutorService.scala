package util

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

class ExecutorFromExecutorService(executorService: ExecutorService) extends Executor {
  def execute(command: Runnable): Unit = {
    executorService.execute(command)
  }
}
