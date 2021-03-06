package com.sksamuel.pulsar4s.zio

import java.util.concurrent.CompletionStage

import com.sksamuel.pulsar4s.{AsyncHandler, ConsumerMessage, DefaultProducer, MessageId, Producer}
import org.apache.pulsar.client.api
import org.apache.pulsar.client.api.{Consumer, Reader, TypedMessageBuilder}
import org.apache.pulsar.client.api.ProducerBuilder
import zio.{Task, ZIO}

import scala.util.Try

class ZioAsyncHandler extends AsyncHandler[Task] {

  private def fromFuture[T](javaFutureTask: Task[CompletionStage[T]]): Task[T] =
    javaFutureTask >>= (cs => ZIO.fromCompletionStage(cs))

  override def transform[A, B](task: Task[A])(fn: A => Try[B]): Task[B] =
    task >>= { v => Task.fromTry(fn(v)) }

  override def failed(e: Throwable): Task[Nothing] =
    Task.fail(e)

  override def createProducer[T](builder: ProducerBuilder[T]): Task[Producer[T]] =
    fromFuture(Task(builder.createAsync())) >>= (p => Task(new DefaultProducer(p)))

  override def send[T](t: T, producer: api.Producer[T]): Task[MessageId] =
    fromFuture(Task(producer.sendAsync(t))).map(MessageId.fromJava)

  override def send[T](builder: TypedMessageBuilder[T]): Task[MessageId] =
    fromFuture(Task(builder.sendAsync())).map(MessageId.fromJava)

  override def receive[T](consumer: api.Consumer[T]): Task[ConsumerMessage[T]] =
    fromFuture(Task(consumer.receiveAsync())) >>= (v => Task(ConsumerMessage.fromJava(v)))

  override def close(producer: api.Producer[_]): Task[Unit] =
    fromFuture(Task(producer.closeAsync())).unit

  override def close(consumer: api.Consumer[_]): Task[Unit] =
    fromFuture(Task(consumer.closeAsync())).unit

  override def close(reader: Reader[_]): Task[Unit] =
    fromFuture(Task(reader.closeAsync())).unit

  override def flush(producer: api.Producer[_]): Task[Unit] =
    fromFuture(Task(producer.flushAsync())).unit

  override def seekAsync(consumer: api.Consumer[_], messageId: MessageId): Task[Unit] =
    fromFuture(Task(consumer.seekAsync(messageId))).unit

  override def nextAsync[T](reader: Reader[T]): Task[ConsumerMessage[T]] =
    fromFuture(Task(reader.readNextAsync())) >>= (v => Task(ConsumerMessage.fromJava(v)))

  override def unsubscribeAsync(consumer: api.Consumer[_]): Task[Unit] =
    fromFuture(Task(consumer.unsubscribeAsync())).unit

  override def acknowledgeAsync[T](consumer: api.Consumer[T], messageId: MessageId): Task[Unit] =
    fromFuture(Task(consumer.acknowledgeAsync(messageId))).unit

  override def negativeAcknowledgeAsync[T](consumer: Consumer[T], messageId: MessageId): Task[Unit] =
    Task(consumer.negativeAcknowledge(messageId))

  override def acknowledgeCumulativeAsync[T](consumer: api.Consumer[T], messageId: MessageId): Task[Unit] =
    fromFuture(Task(consumer.acknowledgeCumulativeAsync(messageId))).unit

}

object ZioAsyncHandler {
  implicit def handler: AsyncHandler[Task] = new ZioAsyncHandler
}
