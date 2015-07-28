package com.github.dnvriend

import java.io.InputStream

import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import com.scalawilliam.xs4s.XmlStreamElementProcessor

import scala.util.Try
import scala.xml.{Elem, NodeSeq}

trait CouvertsDomain
case class Reviews(xs: List[Review]) // wrapper for type erasure on collections
case class Keukens(xs: List[String]) // wrapper for type erasure on collections
case class Review(datum: String, door: String, gemiddeldeScore: Double, titel: String, bericht: String) extends CouvertsDomain
case class Adres(straat: String, huisnummer: String, postcode: String, stad: String) extends CouvertsDomain
case class Restaurant(id: Long, naam: String, adres: Option[Adres], telefoon: String, keukens: List[String], reviews: List[Review], gemiddeldeScore: Double, gemiddeldePrijsHoofdMenu: Double, couvertsURL: String, adWordsNaam: String) extends CouvertsDomain

class IterateOverPartnerFeedTest extends TestSpec {

  type RestaurantId = Long
  type Xml = Elem

  def preProcessId: Flow[Xml, (RestaurantId, Xml), Unit] =
    Flow[Elem].map(xml => ((xml \ "Id").text.toLong, xml))

  def processRestaurant: Flow[(RestaurantId, Xml), (RestaurantId, Restaurant), Unit] =
    Flow[(RestaurantId, Xml)].map {
      case (restaurantId, xml) => (
          restaurantId,
          Restaurant(
            restaurantId,
            (xml \ "Naam").text, None,
            (xml \ "Telefoon").text,
            Nil,
            Nil,
            (xml \ "GemiddeldeScore").text.toDouble,
            (xml \ "GemiddeldePrijsHoofdMenu").text.toDouble,
            (xml \ "CouvertsURL").text,
            (xml \ "AdWordsNaam").text)
        )
    }

  def processIdentity: Flow[(RestaurantId, Xml), (RestaurantId, Xml), Unit] = Flow[(RestaurantId, Xml)].map(identity)

  def mapKeukens(seq: NodeSeq): Keukens = Keukens(seq.map(_.text).toList)

  def processKeukens = Flow[(RestaurantId, Xml)].map {
    case (restaurantId, xml) => (restaurantId, mapKeukens(xml \\ "Keuken"))
  }

  def processAdres:  Flow[(RestaurantId, Xml), (RestaurantId, Adres), Unit] = Flow[(RestaurantId, Xml)].map {
    case (restaurantId, xml) =>
      val adresXml = xml \ "Adres"
      (restaurantId, Adres((adresXml \ "Straat").text, (adresXml \ "Huisnummer").text, (adresXml \ "Postcode").text, (adresXml \ "Stad").text))
  }

  def mapReviews(seq: NodeSeq): List[Review] = {
    seq.map(xml => Review((xml \ "Datum").text, (xml \ "Door").text, Try((xml \ "GemiddeldeScore").text.toDouble).getOrElse(0.0), (xml \ "Titel").text, (xml \ "Bericht").text)).toList
  }

  def processReview:  Flow[(RestaurantId, Xml), (RestaurantId, Reviews), Unit] = Flow[(RestaurantId, Xml)].map {
    case (restaurantId, xml) => (restaurantId, Reviews(mapReviews(xml \\ "Review")))
  }

  def fanOutAndMerge = Flow() { implicit b =>
    import FlowGraph.Implicits._
    val nrPorts = 5
    val bcast = b.add(Broadcast[(RestaurantId, Xml)](nrPorts))
    val merge = b.add(Merge[(RestaurantId, AnyRef)](nrPorts))
    bcast ~> processIdentity ~> merge
    bcast ~> processAdres ~> merge
    bcast ~> processReview ~> merge
    bcast ~> processRestaurant ~> merge
    bcast ~> processKeukens ~> merge
    (bcast.in, merge.out)
  }

  val splitter = XmlStreamElementProcessor.collectElements { _.last == "Restaurant" }

  def groupById = Flow[(RestaurantId, AnyRef)].groupBy {
    case (restaurantId, _) => restaurantId
  }

  "partnerfeed" should "iterate over xml" in {
    withInputStream(TestPartnerFeed) { (is: InputStream) =>
      import XmlStreamElementProcessor.IteratorCreator._
      val restaurantIterator: Iterator[Elem] = splitter.processInputStream(is)
      Source(() => restaurantIterator)
        .via(preProcessId)
        .via(fanOutAndMerge)
        .via(groupById)
        .map {
          case (id, stream) => stream.runFold(Restaurant(id, "", None, "", Nil, Nil, 0.0d, 0.0d, "", "")) {
            case (restaurant, (_, adres: Adres)) => restaurant.copy(adres = Option(adres))
            case (restaurant, (_, reviews: Reviews)) => restaurant.copy(reviews = reviews.xs)
            case (restaurant, (_, r: Restaurant)) =>
              restaurant.copy(
                naam = r.naam,
                telefoon = r.telefoon,
                couvertsURL = r.couvertsURL,
                gemiddeldeScore = r.gemiddeldeScore,
                gemiddeldePrijsHoofdMenu = r.gemiddeldePrijsHoofdMenu,
                adWordsNaam = r.adWordsNaam
              )
            case (restaurant, (_, keukens: Keukens)) => restaurant.copy(keukens = keukens.xs)
            case (restaurant, m) => restaurant
          }
        }
        // note, be sure to set the buffer as long as the records you wish to process
        // see: http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0/scala/stream-cookbook.html#Implementing_reduce-by-key
        .buffer(100000, OverflowStrategy.fail)
        .mapAsync(4)(identity)
        .runForeach(println)
        .toTry should be a 'success
    }
  }
}