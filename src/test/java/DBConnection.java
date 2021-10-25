import java.sql.*;

public class DBConnection {


    String connectionUrl =
        "jdbc:h2:tcp://localhost:8091/mem:personDB;"
                + "user=testDBUsername;"
                + "password=testDBPassword;";

    public boolean checkIfDataExists(String query){
        ResultSet resultSet = null;
        try (Connection connection = DriverManager.getConnection(connectionUrl);

             Statement statement = connection.createStatement();) {

            resultSet = statement.executeQuery(query);
            return resultSet.next();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean runQuery(String query){
        try (Connection connection = DriverManager.getConnection(connectionUrl);
             Statement statement = connection.createStatement();) {
             return statement.execute(query);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;

    }



}
