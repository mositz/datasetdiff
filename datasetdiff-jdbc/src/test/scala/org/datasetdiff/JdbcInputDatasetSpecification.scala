package org.datasetdiff

import org.specs.Specification
import org.specs.runner.JUnit4
import java.lang.String
import java.sql._

/**
 * @author agustafson
 */
class JdbcInputDatasetTest extends JUnit4(JdbcInputDatasetSpecification)

object JdbcInputDatasetSpecification extends Specification {
  val databaseName: String = "jdbcTest";
  val baseConnectionUrl: String = "jdbc:derby:" + databaseName
  val jdbcExecutor = new JdbcExecutor(baseConnectionUrl + ";create=true")

  doBeforeSpec {
    jdbcExecutor.executeUpdate("CREATE TABLE TEST (ID INT, DESCRIPTION VARCHAR(255))")
  }

  doAfterSpec {
    jdbcExecutor.ignore {
      () => jdbcExecutor.executeUpdate("DROP TABLE TEST")
    }
    jdbcExecutor.close
    // shutdown
    var gotSQLExc = false;
    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch {
      case se: SQLException =>
        if (se.getSQLState().equals("XJ015")) {
          gotSQLExc = true;
        }
    }
    if (!gotSQLExc) {
      System.out.println("Database did not shut down normally");
    }
  }

  "JdbcInputDataset" should {
    "extract a simple dataset" in {
      val insertSql = "INSERT INTO TEST (ID, DESCRIPTION) VALUES (?,?)"
      jdbcExecutor.executeUpdate(insertSql, 1, "a")
      jdbcExecutor.executeUpdate(insertSql, 2, "b")
      jdbcExecutor.executeQuery("SELECT * FROM TEST") {
        (resultSet: ResultSet) => {
          val jdbcInputDataset: JdbcInputDataset = new JdbcInputDataset(resultSet)
          val results = jdbcInputDataset.extractDataRows()

          val expectedResults = List(Seq(1, "a"), Seq(2, "b"))
          results.toList must haveTheSameElementsAs(expectedResults)
        }
      }
    }
  }
}