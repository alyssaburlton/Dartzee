package dartzee.logging

import com.amazonaws.auth.BasicAWSCredentials
import java.nio.charset.Charset
import java.util.*

const val ELASTICSEARCH_URL = "https://search-dartzee-nfqeufkxsx6cu7sybhm53dts7e.eu-west-2.es.amazonaws.com"
private const val INDEX_PATH = "dartzee"

object LoggerFactory
{
    fun constructElasticsearchDestination(): LogDestinationElasticsearch
    {
        val poster = readCredentials()?.let { ElasticsearchPoster(it, ELASTICSEARCH_URL, INDEX_PATH) }
        return LogDestinationElasticsearch(poster)
    }

    fun readCredentials(): BasicAWSCredentials?
    {
        try
        {
            val awsCredentials = javaClass.getResource("/aws").readText()
            val decoded = Base64.getDecoder().decode(awsCredentials).toString(Charset.forName("UTF-8"))
            val lines = decoded.lines()
            return BasicAWSCredentials(lines[0], lines[1])
        }
        catch (t: Throwable)
        {
            println("Failed to read in AWS credentials: $t")
            t.printStackTrace()
            return null
        }
    }
}