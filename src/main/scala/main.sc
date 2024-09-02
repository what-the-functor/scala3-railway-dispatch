import scala.collection.immutable.HashMap
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.util.Try

case class Line[A: ClassTag](position: Int = 0, first: A, second: A, rest: A*) { self =>
  val path: ArraySeq[A] =
    ArraySeq.from(first :: second :: rest.toList)

  val last: A = path.last
  val lastId: Int = count - 1

  def count: Int = path.size

  def validateId(id: Int): Either[String, Int] =
    Right(id)
      .filterOrElse(_ >= 0, s"ID must be unsigned")
      .filterOrElse(_ <= lastId, s"ID out of bounds. The line has $count terminals")

  def nextId(id: Int): Either[String, Int] =
    Right(id)
      .flatMap(validateId)
      .filterOrElse(_ != lastId, "This is the last terminal")
      .map(_ + 1)

  def prevId(id: Int): Either[String, Int] =
    Right(id)
      .flatMap(validateId)
      .filterOrElse(_ != 0, "This is the first terminal")
      .map(_ - 1) 

  def next: Either[String, Line[A]] =
    nextId(position)
      .map(x => Line(x, first, second, rest: _*))

  def prev: Either[String, Line[A]] =
    prevId(position)
      .map(x => Line(x, first, second, rest: _*))
}

object Line {
  def two[A: ClassTag](first: A, last: A): Line[A] = Line(0, first, last)
}
  
case class Lines[A](first: Line[A], rest: Line[A]*) {
  def all: ArraySeq[Line[A]] = ArraySeq.from(first :: rest.toList)
}


val line = Line.two(1, 2)
val line2 = Line(0, 1, 2, 3, 4)

val lines = Lines(line, line2)

line.next.flatMap(_.prev).flatMap(_.next)

lines.all.map(_.next)



