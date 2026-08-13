package ltmp.charactergen

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import malibu.tracer.TracerConfigurer
import malibu.tracer.TracerContextApplyer
import malibu.tracer.webclient.TracerWebClientConfiguration
import malibu.tracer.webclient.TracerWebClientConfigurer
import malibu.tracer.webclient.TracerWebClientContextApplier
import malibu.tracer.webmvc.TracerWebMvcConfiguration
import malibu.tracer.webmvc.TracerWebMvcConfigurer
import malibu.tracer.webmvc.TracerWebMvcContextApplier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(*[
    TracerWebMvcConfiguration::class,
    TracerWebClientConfiguration::class,
])
@SpringBootApplication
class CharacterGen2Application(
    @param:Value("\${OPENAI_API_KEY}")
    private val openaiApiKey: String,
) {

    @Bean
    fun openaiClient(): OpenAIClient {
        return OpenAIOkHttpClient.builder()
            .apiKey(openaiApiKey)
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<CharacterGen2Application>(*args)
}

@Configuration
class LtmpTracerConfiguration(

): TracerConfigurer, TracerWebClientConfigurer, TracerWebMvcConfigurer {
    override fun configureTracer(context: TracerContextApplyer) {
//        context.domain = "character-gen"
    }

    override fun configureTracerWebClient(context: TracerWebClientContextApplier) {
        context.traceRequestHeaders = true
        context.traceRequestBody = true
        context.traceResponseBody = true
    }

    override fun configureTracerWebMvc(context: TracerWebMvcContextApplier) {
        context.traceRequestBody = true
        context.traceResponseBody = true
    }
}