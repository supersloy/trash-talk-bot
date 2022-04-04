package trash.persistence.model

import scala.collection.immutable.SortedMap

sealed trait MsgType

object MsgType {
  final case object TEXT extends MsgType

  final case object IMAGE extends MsgType

  final case object STICKER extends MsgType

  final case object VIDEO extends MsgType

  final case object DOC extends MsgType

  private[persistence] val mapping = SortedMap(
    "TEXT"    -> TEXT,
    "IMAGE"   -> IMAGE,
    "STICKER" -> STICKER,
    "VIDEO"   -> VIDEO,
    "DOC"     -> DOC,
  )

  val msgTypes: List[MsgType] = mapping.values.toList

}
