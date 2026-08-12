package ltmp.charactergen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CharacterGen2Application

fun main(args: Array<String>) {
    runApplication<CharacterGen2Application>(*args)
}
