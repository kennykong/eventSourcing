package com.kenny.command

import com.kenny.event.Event
import scalaz.Free

trait Commands[A] {
  type Command[A] = Free[Event, A]
}

