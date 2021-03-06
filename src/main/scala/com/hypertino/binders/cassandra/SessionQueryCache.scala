package com.hypertino.binders.cassandra

import java.util.concurrent.{Callable, TimeUnit}

import com.datastax.driver.core.Session
import com.google.common.cache.{Cache, CacheBuilder}
import com.hypertino.inflector.naming.Converter

import scala.reflect.runtime.universe._

trait SessionQueryCache[C <: Converter] {
  def session: Session
  def createQuery(query: String): Query[C]
}

class GuavaSessionQueryCache[C <: Converter : TypeTag](
                                                   val session: Session,
                                                   protected val cache: Cache[String, Query[C]])
  extends SessionQueryCache[C] {

  def this(aSession: Session, cacheSize: Int = 4096, expireAfterAccessInSeconds: Int = 8 * 3600) = this(
    session = aSession,
    cache = CacheBuilder.newBuilder()
      .expireAfterAccess(expireAfterAccessInSeconds, TimeUnit.SECONDS)
      .maximumSize(cacheSize)
      .build()
  )

  def createQuery(query: String): Query[C] =
    cache.get(query, new Loader(query))

  protected def newQuery(query: String) = new Query[C](session, query)

  protected class Loader(query: String) extends Callable[Query[C]] {
    override def call(): Query[C] = newQuery(query)
  }
}
