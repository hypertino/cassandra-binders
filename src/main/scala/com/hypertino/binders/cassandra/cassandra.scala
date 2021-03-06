package com.hypertino.binders

import com.datastax.driver.core.{BatchStatement ⇒ DriverBatchStatement}
import com.hypertino.binders.cassandra.internal.CqlMacro
import com.hypertino.inflector.naming.Converter
import monix.eval.Task
import monix.execution.Scheduler

import scala.language.experimental.macros
import scala.reflect.runtime.universe._


package object cassandra {

  implicit class CqlContext(val sc: StringContext) extends AnyVal {
    def cql[C <: Converter : SessionQueryCache](args: Any*): Statement[C] = macro CqlMacro.cql[C]
  }

  implicit class StatementOps[S <: AbstractStatement[_, _]](val stmt: S) extends AnyVal {
    def one[O](implicit scheduler: Scheduler): Task[O] = macro CqlMacro.one[S, O]

    def oneApplied[O](implicit scheduler: Scheduler): Task[IfApplied[O]] = macro CqlMacro.oneApplied[S, O]

    def oneOption[O](implicit scheduler: Scheduler): Task[Option[O]] = macro CqlMacro.oneOption[S, O]

    def all[O](implicit scheduler: Scheduler): Task[Iterator[O]] = macro CqlMacro.all[S, O]
  }

  //implicit def convertFutureToUnit[R <: Rows[_]](f: Future[R])(implicit executor: ExecutionContext): Future[Unit] = f.map(_ ⇒ {})

  object Batch {

    def apply[C  <: Converter : SessionQueryCache : TypeTag](statements: Statement[_] *)(implicit cache: SessionQueryCache[_]): BatchStatement[C] = {
      logged(statements: _*)
    }

    def logged[C  <: Converter : SessionQueryCache : TypeTag](statements: Statement[_] *)(implicit cache: SessionQueryCache[_]): BatchStatement[C] = {
      batchWithType[C](DriverBatchStatement.Type.LOGGED, statements: _*)
    }

    def unlogged[C  <: Converter : SessionQueryCache : TypeTag](statements: Statement[_] *)(implicit cache: SessionQueryCache[_]): BatchStatement[C] = {
      batchWithType[C](DriverBatchStatement.Type.UNLOGGED, statements: _*)
    }

    def counter[C  <: Converter : SessionQueryCache : TypeTag](statements: Statement[_] *)(implicit cache: SessionQueryCache[_]): BatchStatement[C] = {
      batchWithType[C](DriverBatchStatement.Type.COUNTER, statements: _*)
    }

    private def batchWithType[C  <: Converter : SessionQueryCache : TypeTag](batchType: DriverBatchStatement.Type, statements: Statement[_] *)(implicit cache: SessionQueryCache[_]): BatchStatement[C] = {
      val driverStatements = statements.map(_.boundStatement)
      new BatchStatement[C](cache.session, batchType, driverStatements: _*)
    }
  }
}
