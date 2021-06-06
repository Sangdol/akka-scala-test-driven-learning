package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.example.GreeterMain.SayHello

object Greeter {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String, from: ActorRef[Greet])

  // Behaviors.receive: behavior factory
  def apply(): Behavior[Greet] =
    Behaviors.receive { (context, message) =>
      context.log.info("Hello {}!", message.whom)
      // !: bang or tell
      message.replyTo ! Greeted(message.whom, context.self)
      // No need to update any state so it returns `same`.
      // What could be other options?
      Behaviors.same
    }
}

object GreeterBot {

  def apply(max: Int): Behavior[Greeter.Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[Greeter.Greeted] =
    Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      context.log.info("Greeting {} for {}", n, message.whom)
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! Greeter.Greet(message.whom, context.self)
        bot(n, max)
      }
    }
}

object GreeterMain {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      // spawn: actor factory
      val greeter: ActorRef[Greeter.Greet] = context.spawn(Greeter(), "greeter")

      Behaviors.receiveMessage { message =>
        val replyTo = context.spawn(GreeterBot(max = 3), message.name)
        greeter ! Greeter.Greet(message.name, replyTo)

        // Does it have to be `same`?
        Behaviors.same
      }
    }
}

object AkkaQuickstart extends App {
  // ActorSystem: the entry point into Akka
  // GreeterMain:  a guardian actor to bootstrap
  val greeterMain: ActorSystem[GreeterMain.SayHello] =
    ActorSystem(GreeterMain(), "AkkaQuickStart")

  greeterMain ! SayHello("Charles")
}
