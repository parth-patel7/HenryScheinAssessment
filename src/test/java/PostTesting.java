import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

public class PostTesting {

    DBConnection connection = new DBConnection();

    public Object connect(String link, String username, String password, String json) throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if(username != null && password != null && username != "" && password != "") {
            String authorization =
                    Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("authorization", "Basic " + authorization);
        }
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        try(OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes());
        }

        ObjectMapper mapper = new ObjectMapper();
        if (connection.getResponseCode() == 200 || connection.getResponseCode() == 201) {
            InputStream responseStream = connection.getInputStream();
            Person p = mapper.readValue(responseStream, Person.class);
            return p;
        } else {
            String errorData = null;
            if(connection.getErrorStream() != null) {
                errorData = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
            return new ErrorObject(connection.getResponseCode(), errorData);
        }
    }

    // With no authentication/authorization
    @Test
    public void postPersonWithoutAuth() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "", "",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1233211233\"" + " }");
        Assert.assertEquals(401, eo.responseCode);
    }


    // With 'Write' authentication/authorization
    @Test
    public void postPersonWithWriteAuth() throws IOException{
        Person p = (Person) connect("http://localhost:8083/v1/post-person/", "admin", "testPassword",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1233211233\"" + " }");
        Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = " + p.getId()));
        Assert.assertTrue(p.getId() > -1);
        Assert.assertEquals("Test", p.getFirstName());
        Assert.assertEquals("Testing", p.getLastName());
        Assert.assertEquals("1233211233", p.getPhoneNumber());
    }

    // With 'Read' authentication/authorization
    @Test
    public void postPersonWithReadAuth() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "testUsername", "testPassword",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1233211233\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(403, eo.responseCode);
    }


    // With incorrect authentication (Both password and username are incorrect)
    @Test
    public void postPersonWithIncorrectAuth() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "Test", "Test",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1233211233\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(401, eo.responseCode);
    }

    // With incorrect 'Read' and 'Write' authentication (Correct password but incorrect username - both Read and Write have same password)
    @Test
    public void postPersonWithIncorrectUsername() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "Test", "testPassword",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1233211233\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(401, eo.responseCode);
    }

    // With incorrect 'Write' authentication (Incorrect password and incorrect username)
    @Test
    public void postPersonWithIncorrectWritePass() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "admin", "Testing",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1233211233\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(401, eo.responseCode);
    }


    // With incorrect 'Write' authentication (Incorrect password and incorrect username)
    @Test
    public void postPersonWithIncorrectReadPass() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "testUsername", "Testing",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1233211233\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(401, eo.responseCode);
    }


    // With no firstname
    @Test
    public void postPersonWithNoFirstName() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "admin", "testPassword",
                "{" + " \"firstName\": \"\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"1231231231\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(400, eo.responseCode);
        Assert.assertEquals("[\"JSON Error: firstName must not be blank\"]", eo.errorData);
    }

    // With Phone number greater than 10 digits
    @Test
    public void postPersonWithIncorrectPhone() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "admin", "testPassword",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"12345678912\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(400, eo.responseCode);
        Assert.assertEquals("[\"JSON Error: phoneNumber phoneNumber must be 10 digits.\"]", eo.errorData);
    }

    // With Phone number less than 10 digits
    @Test
    public void postPersonWithIncorrectPhone2() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "admin", "testPassword",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"123\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(400, eo.responseCode);
        Assert.assertEquals("[\"JSON Error: phoneNumber phoneNumber must be 10 digits.\"]", eo.errorData);
    }

    // With  no firstname and Phone number greater than 10 digits
    @Test
    public void postPersonWithIncorrectDetails() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/post-person/", "admin", "testPassword",
                "{" + " \"firstName\": \"\"," + "  \"lastName\": \"Testing\"," + "  \"phoneNumber\": \"12345678912\"" + " }");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person"));
        Assert.assertEquals(400, eo.responseCode);
        Assert.assertEquals(true, eo.errorData.contains("\"JSON Error: phoneNumber phoneNumber must be 10 digits.\""));
        Assert.assertEquals(true, eo.errorData.contains("\"JSON Error: firstName must not be blank\""));
    }

    // With firstname but no lastname and phone number
    @Test
    public void postPersonWithNoLastNameAndPhone() throws IOException{
        Person p = (Person) connect("http://localhost:8083/v1/post-person/", "admin", "testPassword",
                "{" + " \"firstName\": \"Test\"," + "  \"lastName\": \"\"," + "  \"phoneNumber\": \"\"" + " }");
        Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = " + p.getId()));
        Assert.assertEquals("Test", p.getFirstName());
    }


    @After
    public void clearDB(){
        String query = "Delete from Person";
        connection.runQuery(query);
    }

}
