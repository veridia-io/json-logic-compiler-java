package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}
import io.veridia.jsonlogic.helpers.DateHelper

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.time.temporal.ChronoField
import java.util as ju

/** Date/timestamp operators, all on a plain epoch-millis `Long`/`Double` representation — never a
  * boxed date/time type — so every existing numeric operator (`>`,`==`,`+`, ...) already works on
  * a date value unmodified. Generators (`now`/`today`/`date_add`/`date_truncate`) return millis;
  * component extractors (`year`/`month`/`day`/`hour`/`day_of_week`) return a plain `Integer` so
  * they compose with `in`/chained comparison without a cross-numeric-type mismatch; `date_diff`
  * returns a signed `Long` magnitude; `date_eq` compares N calendar components for equality.
  */
object DateTime:

  val Now: Operator = new Operator:
    def key(): String = "now"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      // Instant.now() must be read inside eval, never at compile() time: JsonLogic caches this
      // CompiledExpression indefinitely per unique expression string.
      _ => java.lang.Long.valueOf(Instant.now().toEpochMilli)

  val Today: Operator = new Operator:
    def key(): String = "today"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      val tzExpr = if args.isEmpty then None else Some(args.get(0))
      ctx =>
        val zone = DateHelper.resolveZone(tzExpr.map(_.eval(ctx)).orNull)
        val millis = Instant.now().atZone(zone).toLocalDate.atStartOfDay(zone).toInstant.toEpochMilli
        java.lang.Long.valueOf(millis)

  val DateAdd: Operator = new Operator:
    def key(): String = "date_add"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 3 then throw new IllegalArgumentException("Operator 'date_add' expects exactly 3 arguments")
      val valueExpr = args.get(0)
      val amountExpr = args.get(1)
      // unit is a fixed vocabulary string, not a per-evaluation value — resolved once here.
      val unit = DateHelper.toChronoUnit(String.valueOf(args.get(2).eval(ju.Collections.emptyMap())))
      ctx =>
        val value = DateHelper.toMillis(valueExpr.eval(ctx))
        val amount = DateHelper.toMillis(amountExpr.eval(ctx))
        val zdt = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC)
        java.lang.Long.valueOf(zdt.plus(amount, unit).toInstant.toEpochMilli)

  val DateTruncate: Operator = new Operator:
    def key(): String = "date_truncate"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() < 2 then throw new IllegalArgumentException("Operator 'date_truncate' expects at least 2 arguments")
      val valueExpr = args.get(0)
      val unit = DateHelper.toChronoUnit(String.valueOf(args.get(1).eval(ju.Collections.emptyMap())))
      val tzExpr = if args.size() > 2 then Some(args.get(2)) else None
      ctx =>
        val zone = DateHelper.resolveZone(tzExpr.map(_.eval(ctx)).orNull)
        val zdt = DateHelper.toZonedDateTime(valueExpr.eval(ctx), zone)
        java.lang.Long.valueOf(DateHelper.truncateTo(zdt, unit).toInstant.toEpochMilli)

  val DateDiff: Operator = new Operator:
    def key(): String = "date_diff"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 3 then throw new IllegalArgumentException("Operator 'date_diff' expects exactly 3 arguments")
      val aExpr = args.get(0)
      val bExpr = args.get(1)
      val unit = DateHelper.toChronoUnit(String.valueOf(args.get(2).eval(ju.Collections.emptyMap())))
      ctx =>
        val a = DateHelper.toZonedDateTime(aExpr.eval(ctx), ZoneOffset.UTC)
        val b = DateHelper.toZonedDateTime(bExpr.eval(ctx), ZoneOffset.UTC)
        java.lang.Long.valueOf(unit.between(a, b))

  private val DefaultDateEqComponents: ju.List[String] = ju.List.of("year", "month", "day")

  val DateEq: Operator = new Operator:
    def key(): String = "date_eq"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() < 2 then throw new IllegalArgumentException("Operator 'date_eq' expects at least 2 arguments")
      val aExpr = args.get(0)
      val bExpr = args.get(1)
      // components is a literal array, not a per-evaluation value — resolved once here.
      val rawComponents: ju.List[?] =
        if args.size() > 2 then args.get(2).eval(ju.Collections.emptyMap()).asInstanceOf[ju.List[?]]
        else DefaultDateEqComponents
      val fields: Array[ChronoField] = rawComponents.toArray().map(c => DateHelper.toChronoField(String.valueOf(c)))
      val tzExpr = if args.size() > 3 then Some(args.get(3)) else None
      ctx =>
        val zone = DateHelper.resolveZone(tzExpr.map(_.eval(ctx)).orNull)
        val a = DateHelper.toZonedDateTime(aExpr.eval(ctx), zone)
        val b = DateHelper.toZonedDateTime(bExpr.eval(ctx), zone)
        java.lang.Boolean.valueOf(fields.forall(f => a.get(f) == b.get(f)))

  private def extractor(sym: String, field: ChronoField): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.isEmpty then throw new IllegalArgumentException(s"Operator '$sym' expects at least 1 argument")
      val valueExpr = args.get(0)
      val tzExpr = if args.size() > 1 then Some(args.get(1)) else None
      ctx =>
        val zone = DateHelper.resolveZone(tzExpr.map(_.eval(ctx)).orNull)
        val zdt = DateHelper.toZonedDateTime(valueExpr.eval(ctx), zone)
        java.lang.Integer.valueOf(zdt.get(field))

  // ISO-8601 numbering for day_of_week (Mon=1..Sun=7) — ChronoField.DAY_OF_WEEK's default under
  // ISO chronology.
  val Year: Operator = extractor("year", ChronoField.YEAR)
  val Month: Operator = extractor("month", ChronoField.MONTH_OF_YEAR)
  val Day: Operator = extractor("day", ChronoField.DAY_OF_MONTH)
  val Hour: Operator = extractor("hour", ChronoField.HOUR_OF_DAY)
  val DayOfWeek: Operator = extractor("day_of_week", ChronoField.DAY_OF_WEEK)

  val all: List[Operator] =
    List(Now, Today, DateAdd, DateTruncate, DateDiff, DateEq, Year, Month, Day, Hour, DayOfWeek)
