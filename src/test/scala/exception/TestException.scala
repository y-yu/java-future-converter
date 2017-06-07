package exception

class TestException(message: String = null, cause: Throwable = null)
  extends Exception(message, cause)