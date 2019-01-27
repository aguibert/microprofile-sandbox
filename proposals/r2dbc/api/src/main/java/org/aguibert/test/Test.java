package org.aguibert.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.microprofile.r2dbc.client.R2dbc;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import static org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams.*;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;

public class Test {
	
	public static void main(String args[]) throws Exception {
		new Test().go();
	}
	
	private void go() throws Exception {
        // Create a stream of words
        ReactiveStreams.of("hello", "from", "smallrye", "reactive", "stream", "operators")
                .map(String::toUpperCase) // Transform the words
                .filter(s -> s.length() > 4) // Filter items
                .forEach(word -> System.out.println(">> " + word)) // Terminal operation
                .run(); // Run it (create the streams, subscribe to it...)
        
        Connection con = initDB();
        Statement stmt = con.createStatement();
        try {
        	stmt.execute("CREATE TABLE PERSON (id int, name varchar(255))");
        	System.out.println("Created table PERSON");
        } catch (SQLException e) {
        	System.out.println("Table already existed... " + e.getMessage());
        }
        stmt.execute("DELETE FROM PERSON");
        stmt.execute("INSERT INTO PERSON(id,name) VALUES(1, 'Andy')");
        
        H2ConnectionConfiguration h2Config = H2ConnectionConfiguration.builder()
        		.inMemory("test")
        		.build();
        
        R2dbc r2 = new R2dbc(new H2ConnectionFactory(h2Config));
        r2.inTransaction(h -> {
        	return concat(
        	h.execute("INSERT INTO PERSON(id,name) VALUES(2, 'Bob')"),
        	h.select("SELECT * FROM PERSON")
        			.mapResult(result -> {
        				System.out.println("mapResult: " + result);
        				return result.map((row, md) -> row.get("name"));	
        			}));
        })
        .forEach(System.out::println)
        	.run();
        
        Thread.sleep(3000);
        System.out.println("done");
	}
	
	private Connection initDB() throws Exception {
		Class.forName("org.h2.Driver");
		Connection con = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "");
		System.out.println("Got connection: " + con);
		return con;
	}
	
}
