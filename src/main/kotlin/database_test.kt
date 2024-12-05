import org.bson.Document
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.runBlocking

fun main() {
    // MongoDB Atlas connection string (replace with your own if necessary)
    val connectionString = "mongodb+srv://danteberouty01:kFKabBHS2enDWbAg@cluster0.zuwrj.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0"

    // Set up the server API and MongoClient settings
    val serverApi = ServerApi.builder()
        .version(ServerApiVersion.V1)
        .build()

    val mongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(connectionString))
        .serverApi(serverApi)
        .build()

    // Create the MongoClient and run a simple test
    MongoClient.create(mongoClientSettings).use { mongoClient ->
        val database = mongoClient.getDatabase("admin")
        runBlocking {
            try {
                // Run a simple command to test the connection
                database.runCommand(Document("ping", 1))
                println("Pinged your deployment. You successfully connected to MongoDB!")
            } catch (e: Exception) {
                println("Failed to connect to MongoDB: ${e.message}")
            }
        }
    }
}
