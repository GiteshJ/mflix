package mflix.api.daos;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import static com.mongodb.client.model.Updates.*;


import java.text.MessageFormat;
import java.util.Map;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

    private final MongoCollection<User> usersCollection;
    //TODO> Ticket: User Management - do the necessary changes so that the sessions collection
    //returns a Session object
    private final MongoCollection<Session> sessionsCollection;

    private final Logger log;

    @Autowired
    public UserDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        CodecRegistry pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
        log = LoggerFactory.getLogger(this.getClass());
        //TODO> Ticket: User Management - implement the necessary changes so that the sessions
        // collection returns a Session objects instead of Document objects.
        sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
    }

    /**
     * Inserts the `user` object in the `users` collection.
     *
     * @param user - User object to be added
     * @return True if successful, throw IncorrectDaoOperation otherwise
     */
    public boolean addUser(User user) {
        //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
    	try {
    	      usersCollection.withWriteConcern(WriteConcern.MAJORITY).insertOne(user);
    	    } catch (MongoException e) {
    	      log.error("An error ocurred while trying to insert a User.");
    	      if (ErrorCategory.fromErrorCode( e.getCode() ) == ErrorCategory.DUPLICATE_KEY) {
    	        throw new IncorrectDaoOperation("The User is already in the database.");
    	      }
    	      return false;
    	    }
    	    
    	    return true;
        //TODO > Ticket: Handling Errors - make sure to only add new users
        // and not users that already exist.

    }

    /**
     * Creates session using userId and jwt token.
     *
     * @param userId - user string identifier
     * @param jwt    - jwt string token
     * @return true if successful
     */
    public boolean createUserSession(String userId, String jwt) {
        //TODO> Ticket: User Management - implement the method that allows session information to be
        // stored in it's designated collection.
    	Bson updateFilter = new Document("user_id", userId);
        Bson setUpdate = Updates.set("jwt", jwt);
        UpdateOptions options = new UpdateOptions().upsert(true);
        try {
            sessionsCollection.updateOne(updateFilter, setUpdate, options);
        }
        catch (MongoException e) {
            log.error("An error ocurred while trying to create user session.");
            return false;
          }
        return true;
        //TODO > Ticket: Handling Errors - implement a safeguard against
        // creating a session with the same jwt token.
    }

    /**
     * Returns the User object matching the an email string value.
     *
     * @param email - email string to be matched.
     * @return User object or null.
     */
    public User getUser(String email) {
        //TODO> Ticket: User Management - implement the query that returns the first User object.
        return usersCollection.find(new Document("email", email)).limit(1).first();
    }

    /**
     * Given the userId, returns a Session object.
     *
     * @param userId - user string identifier.
     * @return Session object or null.
     */
    public Session getUserSession(String userId) {
        //TODO> Ticket: User Management - implement the method that returns Sessions for a given
        // userId
        return sessionsCollection.find(new Document("user_id", userId)).limit(1).first();
    }

    public boolean deleteUserSessions(String userId) {
        //TODO> Ticket: User Management - implement the delete user sessions method
    	Document sessionDeleteFilter = new Document("user_id", userId);
        DeleteResult res = sessionsCollection.deleteOne(sessionDeleteFilter);
        if (res.getDeletedCount() < 1) {
          log.warn("User `{}` could not be found in sessions collection.", userId);
          }

          return res.wasAcknowledged();
    }

    /**
     * Removes the user document that match the provided email.
     *
     * @param email - of the user to be deleted.
     * @return true if user successfully removed
     */
    public boolean deleteUser(String email) {
        // remove user sessions
        //TODO> Ticket: User Management - implement the delete user method
    	if (deleteUserSessions(email)) {
    	      Document userDeleteFilter = new Document("email", email);
    	      try {
    	    	  DeleteResult res = usersCollection.deleteOne(userDeleteFilter);
    	      
	    	      if (res.getDeletedCount() < 0) {
	    	        log.warn("User with `email` {} not found. Potential concurrent operation?!");
	    	      }

	    	      return res.wasAcknowledged();
    	      }
    	      catch (MongoException e) {
    	            log.error("An error ocurred while trying to delete user.");
    	            return false;
    	          }
    	    }
        //TODO > Ticket: Handling Errors - make this method more robust by
    	
    	return false;
    }

    /**
     * Updates the preferences of an user identified by `email` parameter.
     *
     * @param email           - user to be updated email
     * @param userPreferences - set of preferences that should be stored and replace the existing
     *                        ones. Cannot be set to null value
     * @return User object that just been updated.
     */
    public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
        //TODO> Ticket: User Preferences - implement the method that allows for user preferences to
        // be updated.
    	
    	User user = getUser(email);
    	if(user!=null && userPreferences!=null ) {
    		usersCollection.updateOne(Filters.eq("email", email), set("preferences",userPreferences));
    		return true;
    	}
    	else {
    		throw new IncorrectDaoOperation(email);
    	}
        //TODO > Ticket: Handling Errors - make this method more robust by
        // handling potential exceptions when updating an entry.
        
    }
}
