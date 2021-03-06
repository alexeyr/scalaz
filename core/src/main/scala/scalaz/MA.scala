package scalaz

sealed trait MA[M[_], A] extends PimpedType[M[A]] {
  import Scalaz._

  def ∘[B](f: A => B)(implicit t: Functor[M]): M[B] = t.fmap(value, f)

  def ∘∘[N[_], B, C](f: B => C)(implicit m: A <:< N[B], f1: Functor[M], f2: Functor[N]): M[N[C]] = ∘(k => (k: N[B]) ∘ f)

  /**
   * Returns a MA with the type parameter `M` equal to [A] M[N[A]], given that type `A` is constructed from type constructor `N`.
   * This allows composition of type classes for `M` and `N`. For example:
   * <code>(List(List(1)).comp.map {2 +}) assert_≟ List(List(3))</code>
   */
  def comp[N[_], B](implicit n: A <:< N[B], f: Functor[M]): MA[Comp[M, N]#Apply, B] = ma[Comp[M, N]#Apply, B](value ∘ n)

  def map[B](f: A => B)(implicit t: Functor[M]): M[B] = ∘(f)

  def >|[B](f: => B)(implicit t: Functor[M]): M[B] = ∘(_ => f)

  def ⊛[B](b: M[B]) = new ApplicativeBuilder[M, A, B](value, b)

  def |@|[B](b: M[B]) = new ApplicativeBuilder[M, A, B](value, b)

  def <*>[B](f: M[A => B])(implicit a: Apply[M]): M[B] = a(f, value)

  def <**>[B, C](b: M[B])(z: (A, B) => C)(implicit t: Functor[M], a: Apply[M]): M[C] = a(t.fmap(value, z.curried), b)

  def <***>[B, C, D](b: M[B], c: M[C])(z: (A, B, C) => D)(implicit t: Functor[M], a: Apply[M]): M[D] = a(a(t.fmap(value, z.curried), b), c)

  def <****>[B, C, D, E](b: M[B], c: M[C], d: M[D])(z: (A, B, C, D) => E)(implicit t: Functor[M], a: Apply[M]): M[E] = a(a(a(t.fmap(value, z.curried), b), c), d)

  def <*****>[B, C, D, E, F](b: M[B], c: M[C], d: M[D], e: M[E])(z: (A, B, C, D, E) => F)(implicit t: Functor[M], a: Apply[M]): M[F] = a(a(a(a(t.fmap(value, z.curried), b), c), d), e)

  def *>[B](b: M[B])(implicit t: Functor[M], a: Apply[M]): M[B] = <**>(b)((a, b) => b)

  def <*[B](b: M[B])(implicit t: Functor[M], a: Apply[M]): M[A] = <**>(b)((a, b) => a)

  def <|*|>[B](b: M[B])(implicit t: Functor[M], a: Apply[M]): M[(A, B)] = <**>(b)((_, _))

  def <|**|>[B, C](b: M[B], c: M[C])(implicit t: Functor[M], a: Apply[M]): M[(A, B, C)] = <***>(b, c)((_, _, _))

  def <|***|>[B, C, D](b: M[B], c: M[C], d: M[D])(implicit t: Functor[M], a: Apply[M]): M[(A, B, C, D)] = <****>(b, c, d)( (_, _, _, _))

  def <|****|>[B, C, D, E](b: M[B], c: M[C], d: M[D], e: M[E])(implicit t: Functor[M], a: Apply[M]): M[(A, B, C, D, E)] = <*****>(b, c, d, e)((_, _, _, _, _))

  def xmap[B](f: A => B)(g: B => A)(implicit xf: InvariantFunctor[M]): M[B] = xf.xmap(value, f, g)
  
  def ↦[F[_], B](f: A => F[B])(implicit a: Applicative[F], t: Traverse[M]): F[M[B]] =
    traverse(f)

  def traverse[F[_],B](f: A => F[B])(implicit a: Applicative[F], t: Traverse[M]): F[M[B]] =
    t.traverse(f, value)

  def >>=[B](f: A => M[B])(implicit b: Bind[M]): M[B] = b.bind(value, f)

  def ∗[B](f: A => M[B])(implicit b: Bind[M]): M[B] = >>=(f)

  def >>=|[B](f: => M[B])(implicit b: Bind[M]): M[B] = >>=(_ => f)

  def ∗|[B](f: => M[B])(implicit b: Bind[M]): M[B] = >>=|(f)

  def flatMap[B](f: A => M[B])(implicit b: Bind[M]): M[B] = >>=(f)

  def join[B](implicit m: A <:< M[B], b: Bind[M]): M[B] = >>=(m)

  def μ[B](implicit m: A <:< M[B], b: Bind[M]): M[B] = join

  def ∞[B](implicit b: Bind[M]): M[B] = forever

  def forever[B](implicit b: Bind[M]): M[B] = value ∗| value.forever

  def <+>(z: => M[A])(implicit p: Plus[M]): M[A] = p.plus(value, z)

  def +>:(a: A)(implicit s: Semigroup[M[A]], q: Pure[M]): M[A] = s append (q.pure(a), value)

  def <+>:(a: A)(implicit p: Plus[M], q: Pure[M]): M[A] = p.plus(q.pure(a), value)

  def foreach(f: A => Unit)(implicit e: Each[M]): Unit = e.each(value, f)

  def |>|(f: A => Unit)(implicit e: Each[M]): Unit = foreach (f)

  def foldl[B](b: B)(f: (B, A) => B)(implicit r: Foldable[M]): B = r.foldLeft[A, B](value, b, f)

  def foldl1(f: (A, A) => A)(implicit r: Foldable[M]): Option[A] = r.foldl1(value, f)

  def listl(implicit r: Foldable[M]): List[A] = {
    val b = new scala.collection.mutable.ListBuffer[A]
    foldl(())((_, a) => b += a)
    b.toList
  }

  def sum(implicit r: Foldable[M], m: Monoid[A]): A = foldl(m.zero)(m append (_, _))

  def ∑(implicit r: Foldable[M], m: Monoid[A]): A = sum

  def count(implicit r: Foldable[M]): Int = foldl(0)((b, _) => b + 1)

  def ♯(implicit r: Foldable[M]): Int = count

  def len(implicit l: Length[M]): Int = l len value

  def max(implicit r: Foldable[M], ord: Order[A]): Option[A] =
    foldl1((x: A, y: A) => if (x ≩ y) x else y)

  def min(implicit r: Foldable[M], ord: Order[A]): Option[A] =
    foldl1((x: A, y: A) => if (x ≨ y) x else y)

  def longDigits(implicit d: A <:< Digit, t: Foldable[M]): Long =
    foldl(0L)((n, a) => n * 10L + (a: Digit))

  def digits(implicit c: A <:< Char, t: Functor[M]): M[Option[Digit]] =
    ∘((a: A) => (a: Char).digit)

  def sequence[N[_], B](implicit a: A <:< N[B], t: Traverse[M], n: Applicative[N]): N[M[B]] =
    traverse((z: A) => (z: N[B]))

  def traverseDigits(implicit c: A <:< Char, t: Traverse[M]): Option[M[Digit]] = {
    val k = ∘((f: A) => (f: Char)).digits.sequence
    k
  }

  def foldr[B](b: B)(f: (A, => B) => B)(implicit r: Foldable[M]): B = r.foldRight(value, b, f)

  def foldr1(f: (A, => A) => A)(implicit r: Foldable[M]): Option[A] = r.foldr1(value, f)

  def ∑∑(implicit r: Foldable[M], m: Monoid[A]): A = foldr(m.zero)(m append (_, _))

  def foldMap[B](f: A => B)(implicit r: Foldable[M], m: Monoid[B]): B = r.foldMap(value, f)

  def listr(implicit r: Foldable[M]): List[A] = foldr(nil[A])(_ :: _)

  def stream(implicit r: Foldable[M]): Stream[A] = foldr(Stream.empty[A])(Stream.cons(_, _))

  def !!(n: Int)(implicit r: Foldable[M]): A = stream(r)(n)

  def !(n: Int)(implicit i: Index[M]): Option[A] = i.index(value, n)

  def -!-(n: Int)(implicit i: Index[M]): A = this.!(n) getOrElse (error("Index " + n + " out of bounds"))

  def any(p: A => Boolean)(implicit r: Foldable[M]): Boolean = foldr(false)(p(_) || _)

  def ∃(p: A => Boolean)(implicit r: Foldable[M]): Boolean = any(p)

  def all(p: A => Boolean)(implicit r: Foldable[M]): Boolean = foldr(true)(p(_) && _)

  def ∀(p: A => Boolean)(implicit r: Foldable[M]): Boolean = all(p)

  def empty(implicit r: Foldable[M]): Boolean = ∀(_ => false)

  def ∈:(a: A)(implicit r: Foldable[M], eq: Equal[A]): Boolean = element(a)

  def ∋(a: A)(implicit r: Foldable[M], eq: Equal[A]): Boolean = element(a)

  def element(a: A)(implicit r: Foldable[M], eq: Equal[A]): Boolean = ∃(a ≟ _)

  /**
   * Splits the elements into groups that alternatively satisfy and don't satisfy the predicate p.
   */
  def splitWith(p: A => Boolean)(implicit r: Foldable[M]): List[List[A]] =
    foldr((nil[List[A]], none[Boolean]))((a, b) => {
      val pa = p(a)
      (b match {
        case (_, None) => List(List(a))
        case (x, Some(q)) => if (pa == q) (a :: x.head) :: x.tail else List(a) :: x
      }, Some(pa))
    })._1


  /**
   * Selects groups of elements that satisfy p and discards others.
   */
  def selectSplit(p: A => Boolean)(implicit r: Foldable[M]): List[List[A]] =
    foldr((nil[List[A]], false))((a, xb) => xb match {
      case (x, b) => {
        val pa = p(a)
        (if (pa)
          if (b)
            (a :: x.head) :: x.tail else
            List(a) :: x
        else x, pa)
      }
    })._1

  def para[B](b: B, f: (=> A, => M[A], B) => B)(implicit p: Paramorphism[M]): B = p.para(value, b, f)

  def ↣[B](f: A => B)(implicit t: Traverse[M], m: Monoid[B]): B = foldMapDefault(f)
  
  def foldMapDefault[B](f: A => B)(implicit t: Traverse[M], m: Monoid[B]): B = {
    t.traverse[PartialApply1Of2[Const, B]#Apply, A, B](a => Const[B, B](f(a)), value)
  }

  def collapse(implicit t: Traverse[M], m: Monoid[A]): A = ↣(identity[A])

  def =>>[B](f: M[A] => B)(implicit w: Comonad[M]): M[B] = w.fmap(w.cojoin(value), f)

  def copure(implicit p: Copure[M]): A = p copure value

  def ε(implicit p: Copure[M]): A = copure

  def cojoin(implicit j: Cojoin[M]): M[M[A]] = j cojoin value

  def υ(implicit j: Cojoin[M]): M[M[A]] = cojoin

  def <--->(w: M[A])(implicit l: Length[M], ind: Index[M], equ: Equal[A]): Int = {
    def levenshteinMatrix(w: M[A])(implicit l: Length[M], ind: Index[M], equ: Equal[A]): (Int, Int) => Int = {
      val m = mutableHashMapMemo[(Int, Int), Int]

      def get(i: Int, j: Int): Int = if (i == 0) j else if (j == 0) i else {
        lazy val t = this -!- (i - 1)
        lazy val u = w -!- (j - 1)
        lazy val e = t ≟ u

        val g = m {case (a, b) => get(a, b)}
        val a = g(i - 1, j) + 1
        val b = g(i - 1, j - 1) + (if (e) 0 else 1)
        def c = g(i, j - 1) + 1
        if (a < b) a else if (b <= c) b else c
      }

      get
    }

    val k = levenshteinMatrix(w)
    k(l.len(value), l.len(w))
  }

  def ifM[B](t: => M[B], f: => M[B])(implicit a: Monad[M], b: A <:< Boolean): M[B] = ∗ ((x: A) => if (x) t else f)

  def foldLeftM[N[_], B](b: B)(f: (B, A) => N[B])(implicit fr: Foldable[M], m: Monad[N]): N[B] =
    foldl[N[B]](b η)((b, a) => b ∗ ((z: B) => f(z, a)))

  def foldRightM[N[_], B](b: B)(f: (B, A) => N[B])(implicit fr: Foldable[M], m: Monad[N]): N[B] =
    foldr[N[B]](b η)((a, b) => b ∗ ((z: B) => f(z, a)))

  def replicateM[N[_]](n: Int)(implicit m: Monad[M], p: Pure[N], d: Monoid[N[A]]): M[N[A]] =
    if (n <= 0) ∅[N[A]].η[M]
    else value ∗ (a => replicateM[N](n - 1) ∘ (a +>: _) )

  def zipWithA[F[_], B, C](b: M[B])(f: (A, B) => F[C])(implicit a: Applicative[M], t: Traverse[M], z: Applicative[F]): F[M[C]] =
    (b <*> (a.fmap(value, f.curried))).sequence[F, C]

  def bktree(implicit f: Foldable[M], m: MetricSpace[A]) =
    foldl(emptyBKTree[A])(_ + _)

  def fpair(implicit f: Functor[M]): M[(A, A)] = ∘(_.pair)

  def foldReduce[B](implicit f: Foldable[M], r: Reducer[A, B]) = foldMap(_.unit[B])(f, r)
  
  import FingerTree._
  def &:(a: A) = OnL[M,A](a, value)
  
  def :&(a: A) = OnR[M,A](value, a)

  import concurrent._
  def parMap[B](f: A => B)(implicit s: Strategy[Unit], t: Traverse[M]): Promise[M[B]] =
    traverse(f.kleisli[Promise])

  def parBind[B](f: A => M[B])(implicit m: Monad[M], s: Strategy[Unit], t: Traverse[M]): Promise[M[B]] =
    parMap(f).map(((_: MA[M, M[B]]) μ) compose (ma(_)))

  def parZipWith[B, C](bs: M[B])(f: (A, B) => C)(implicit z: Applicative[M], s: Strategy[Unit], t: Traverse[M]): Promise[M[C]] =
    zipWithA(bs)((x, y) => promise(f(x, y)))

  import concurrent.Strategy

  def parM[B](implicit b: A <:< (() => B), m: Functor[M], s: Strategy[B]): () => M[B] =
    () => value ∘ (z => s(z).apply)
}

// Previously there was an ambiguity because (A => B) could be considered as MA[(R => _), A] or MA[(_ => R), A].
// We can probably merge MA and MACofunctor when https://lampsvn.epfl.ch/trac/scala/ticket/3340 is solved.
trait MACofunctor[M[_], A] extends PimpedType[M[A]] {
  def ∙[B](f: B => A)(implicit t: Cofunctor[M]): M[B] = t.comap(value, f)

  /**
   * Alias for {@link scalaz.MACofunctor#∙}
   */
  def comap[B](f: B => A)(implicit t: Cofunctor[M]): M[B] = ∙(f)

  /**
   * Comap the identity function
   */
  def covary[B <: A](implicit t: Cofunctor[M]): M[B] = ∙[B](identity)

  def |<[B](f: => A)(implicit t: Cofunctor[M]): M[B] = ∙((_: B) => f)
}


trait MAsLow {
  implicit def maImplicit[M[_], A](a: M[A]): MA[M, A] = new MA[M, A] {
    val value = a
  }

  implicit def maCofunctorImplicit[M[_], A](a: M[A]): MACofunctor[M, A] = new MACofunctor[M, A] {
    val value = a
  }
}

trait MAs {
  def ma[M[_], A](a: M[A]): MA[M, A] = new MA[M, A] {
    val value = a
  }

  def maCofunctor[M[_], A](a: M[A])(implicit cf: Cofunctor[M]): MACofunctor[M, A] = new MACofunctor[M, A] {
    val value = a
  }
}
