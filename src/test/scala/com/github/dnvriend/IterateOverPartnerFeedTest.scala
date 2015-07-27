package com.github.dnvriend

import java.io.InputStream
import akka.stream.scaladsl._
import com.scalawilliam.xs4s.XmlStreamElementProcessor

import scala.xml.Elem

case class Review(datum: String, door: String, gemiddeldeScore: Double, titel: String, bericht: String)
case class Adres(straat: String, huisnummer: String, postcode: String, stad: String)
case class Restaurant(id: Long, naam: String, adres: Option[Adres], telefoon: String, keukens: List[String], reviews: List[Review], gemiddeldeScore: Double, gemiddeldePrijsHoofdMenu: Double, couvertsURL: String, adWordsNaam: String)

class IterateOverPartnerFeedTest extends TestSpec {

  type RestaurantId = Long
  type Xml = Elem

  def preProcessId: Flow[Xml, (RestaurantId, Xml), Unit] =
    Flow[Elem].map(xml => ((xml \ "Id").text.toLong, xml))

  def processRestaurant: Flow[(RestaurantId, Xml), Restaurant, Unit] =
    Flow[(RestaurantId, Xml)].map {
      case (restaurantId, xml) => Restaurant(
        restaurantId, (xml \ "Naam").text, None, (xml \ "Telefoon").text, Nil, Nil, (xml \ "GemiddeldeScore").text.toDouble, (xml \ "GemiddeldePrijsHoofdMenu").text.toDouble, (xml \ "CouvertsURL").text, (xml \ "AdWordsNaam").text
      )
    }

  def processIdentity:  Flow[(RestaurantId, Xml), (RestaurantId, Xml), Unit] = Flow[(RestaurantId, Xml)].map(identity)

  def processAdres:  Flow[(RestaurantId, Xml), Adres, Unit] = Flow[(RestaurantId, Xml)].map {
    case (restaurantId, xml) => ???
  }

  def processReview:  Flow[(RestaurantId, Xml), List[Review], Unit] = Flow[(RestaurantId, Xml)].map {
    case (restaurantId, xml) => ???
  }

  val splitter = XmlStreamElementProcessor.collectElements { _.last == "Restaurant" }

  "partnerfeed" should "iterate over xml" in {
    withInputStream(TestPartnerFeed) { (is: InputStream) =>
      import XmlStreamElementProcessor.IteratorCreator._
      val restaurantIterator: Iterator[Elem] = splitter.processInputStream(is)
      Source( () => restaurantIterator )
        .via(preProcessId)
        .via(processRestaurant)
        .runForeach(println)
        .toTry should be a 'success
    }
  }
}

/**
<Restaurant>
    <Id>1</Id>
    <Naam>freek's restaurant</Naam>
    <Adres>
      <Straat>markt</Straat>
      <Huisnummer>1</Huisnummer>
      <Postcode>5611AX</Postcode>
      <Stad>eindhoven</Stad>
    </Adres>
    <Telefoon>0357113011</Telefoon>
    <Beschrijving />
    <Email />
    <Fotos>
      <Foto>http://restaurant.testing.couverts.nl/upload/fotos/1/1de56d88-3fa1-4cfb-9755-0a835a6be8cf.jpg</Foto>
      <Foto>http://restaurant.testing.couverts.nl/upload/fotos/1/3ffbd979-41d3-4bef-a203-beb4959031d1.jpg</Foto>
      <Foto>http://restaurant.testing.couverts.nl/upload/fotos/1/3cb6fa5f-9327-426b-aa71-9b3dbc95d11a.jpg</Foto>
      <Foto>http://restaurant.testing.couverts.nl/upload/fotos/1/db4d804f-6278-4be6-8c44-b3215a5ed871.jpg</Foto>
      <Foto>http://restaurant.testing.couverts.nl/upload/fotos/1/153d4480-a124-49ba-8a78-dea1e9bebcc9.jpg</Foto>
      <Foto>http://restaurant.testing.couverts.nl/upload/fotos/1/9cc11b58-1700-4783-b8ac-e363b49582d5.jpg</Foto>
    </Fotos>
    <Keukens>
      <Keuken>Aziatisch</Keuken>
      <Keuken>Bistro</Keuken>
      <Keuken>Fondue</Keuken>
      <Keuken>Pannenkoeken</Keuken>
    </Keukens>
    <MenuKaart>/upload/menukaarten/2012/10/25/420679ec94ff810d88942155fa2f788e-NMock3CheatSheet.pdf</MenuKaart>
    <Reviews>
      <Review>
        <Datum>2015-04-28T13:26:21.6561632</Datum>
        <Door>test bij max</Door>
        <GemiddeldeScore>7.3</GemiddeldeScore>
        <Titel>Geen stijl</Titel>
        <Bericht>We zijn erg benieuwd hoe je etentje was! Laat je mening achter op Couverts.nl. Selecteer het aantal sterren dat jij het restaurant op basis van je eigen ervaring wil toekennen. Vervolgens onderbouw je je keuze zo nauwkeurig mogelijk in het opmerkingenveld. Door je review achter te laten, help je andere consumenten om de juiste restaurantkeuze te maken én weten restaurants waar zij zich in kunnen ontwikkelen. Je mening is dus veel waard!</Bericht>
      </Review>
      <Review>
        <Datum>2015-04-24T17:42:50.8882283</Datum>
        <Door>Freek</Door>
        <GemiddeldeScore>6.0</GemiddeldeScore>
        <Titel>test</Titel>
        <Bericht>qwe</Bericht>
      </Review>
      <Review>
        <Datum>2015-04-24T17:13:53.0787636</Datum>
        <Door>Max</Door>
        <GemiddeldeScore>7.7</GemiddeldeScore>
        <Titel>estseesg</Titel>
        <Bericht>mogelijk in het opmerkingenveld. Door je review achter te laten, help je andere consumenten om de juiste restaurantkeuze te maken én weten restaurants waar zij zich in kunnen ontwikkelen. Je mening is dus veel waard!</Bericht>
      </Review>
      <Review>
        <Datum>2015-04-24T17:10:47.3058781</Datum>
        <Door>test reviewer</Door>
        <GemiddeldeScore>5.0</GemiddeldeScore>
        <Titel>test review</Titel>
        <Bericht>test test</Bericht>
      </Review>
      <Review>
        <Datum>2015-04-22T16:31:11.9572425</Datum>
        <Door>Roelie</Door>
        <GemiddeldeScore>7.7</GemiddeldeScore>
        <Titel>Testen </Titel>
        <Bericht>	Couverts Test</Bericht>
      </Review>
    </Reviews>
    <GemiddeldeScore>6.0</GemiddeldeScore>
    <GemiddeldePrijsHoofdMenu>2.00</GemiddeldePrijsHoofdMenu>
    <CouvertsURL>http://tescouvertsnl.cloudapp.net/restaurant/eindhoven/freeks-restaurant</CouvertsURL>
    <AdWordsNaam>freeksrest</AdWordsNaam>
  </Restaurant>
*/