package insynth
package enumeration
package member
package benchmarks

import dependent._
import insynth.{ enumeration => e }
import memoization._

import insynth.util._
import insynth.util.logging._
import RedBlackTreeWithOperations._
import Structures._

import scala.collection.mutable.{ Map => SMap }

import enumeration.benchmarks._

import org.scalatest._
import org.scalameter.api._

import scala.language.postfixOps
import scala.language.existentials

class RedBlackTreeBenchmarkMember
  extends StructuresBenchmark[MemberDependFinite[(Int, Range, Set[Boolean], Int), Tree]]
  with java.io.Serializable with HasLogger {

  fixtureRun("member", "SciFe", 15, "RBTree")
  
  type EnumType = MemberDependFinite[(Int, Range, Set[Boolean], Int), Tree]

  lazy val missingElements = SMap[Tree, Int]()
  lazy val blackHeights = SMap[Tree, Int]()

  def measureCode(tdEnum: EnumType) = {
    { (size: Int) =>
      for (
        blackHeight <- 1 to (Math.log2(size + 1).toInt + 1);
        enum = tdEnum.getEnum(size - 1, 1 to size, Set(true, false), blackHeight);
        ind <- 0 until enum.size
      ) {
        val startingTree = enum(ind)

        val missing = if (size == 1) 1 else missingElements(startingTree)

        val newTree = enum(ind) insert missing
        val newBlackHeight = blackHeights(newTree)
        val enumBigger = tdEnum.getEnum(size, 1 to size, Set(true, false), newBlackHeight)
        
        enumBigger.member(newTree)
      }
    }

  }

  def warmUp(inEnum: EnumType, maxSize: Int) {
    for (size <- 1 to maxSize) {
      val tdEnumVal = inEnum
      for (
        blackHeight <- 1 to (Math.log2(size + 1).toInt + 1);
        enum = inEnum.getEnum(size, 1 to size, Set(true, false), blackHeight);
        ind <- 0 until enum.size
      ) {
        enum.member(enum(ind))
      }
    }
  }

  override def setUp(size: Int, tdEnum: EnumType, memScope: e.memoization.MemoizationScope) {
    val tdEnumVal = tdEnum
    for (
      bH <- 1 to (Math.log2(size + 1).toInt + 1);
      enum = tdEnum.getEnum(size, 1 to size, Set(true, false), bH);
      ind <- 0 until enum.size
    ) {
      val el = enum(ind)
      blackHeights += el -> RedBlackTrees.blackHeight(el)
    }

    if (size > 1) {
      for (
        bH <- 1 to (Math.log2(size + 1).toInt + 1);
        enum = tdEnum.getEnum(size - 1, 1 to size, Set(true, false), bH);
        ind <- 0 until enum.size
      ) {
        val el = enum(ind)
        missingElements += el -> (1 to size).find(!el.contains(_)).get
      }
    }
  }
  
  def tearDown(size: Int, tdEnum: EnumType, memScope: e.memoization.MemoizationScope) = {
    blackHeights.clear    
    missingElements.clear
  }

  def constructEnumerator(implicit ms: e.memoization.MemoizationScope): EnumType = {
    import RedBlackTreeWithOperations._

    val colorsProducer = new WrapFunctionFin(
      (set: Set[Boolean]) => { new WrapArray(set.toArray) })

    val treesOfSize = new WrapFunctionFin(
      (self: MemberDependFinite[(Int, Range, Set[Boolean], Int), Tree],
        pair: (Int, Range, Set[Boolean], Int)) => {
        val (size, range, colors, blackHeight) = pair

        if (range.size >= size && range.size < 0 || blackHeight < 0) new Empty: MemberFinite[Tree]
        else if (size == 0 && blackHeight == 1 && colors.contains(true)) new Singleton(Leaf): MemberFinite[Tree]
        else if (size > 0 && blackHeight >= 1) {
          val roots = new WrapRange(range)
          val leftSizes = new WrapArray(0 until size toArray)
          val rootColors = colorsProducer(colors)

          val rootLeftSizePairs = new member.ProductFinite(leftSizes, roots)
          val rootLeftSizeColorTuples = new member.ProductFinite(rootLeftSizePairs, rootColors)

          val leftTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
          })

          val rightTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
          })

          val leftRightPairs =
            Product(leftTrees, rightTrees)

          val allNodes = new ChainFinite(rootLeftSizeColorTuples, leftRightPairs)

          val makeTree =
            (p: (((Int, Int), Boolean), (Tree, Tree))) => {
              val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = p

              Node(rootColor, leftTree, currRoot, rightTree)
            }

          val invertTree = {
            (p: Tree) =>
              {
                val Node(rootColor, leftTree, currRoot, rightTree) = p.asInstanceOf[Node]

                (((RedBlackTrees.size(leftTree), currRoot), rootColor), (leftTree, rightTree))
              }
          }

          val mapEnum = new Map[(((Int, Int), Boolean), (Tree, Tree)), Tree](allNodes, makeTree,
            invertTree) with MemberFinite[Tree] with e.memoization.Memoized[Tree] with Memoized[Tree]
          
          ms add mapEnum
          
          mapEnum
        } else new Empty: MemberFinite[Tree]
      }) with e.memoization.dependent.Memoized[(Int, Range, Set[Boolean], Int), Tree]

    ms add treesOfSize
    
    treesOfSize
  }

}