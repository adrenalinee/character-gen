package ltmp.charactergen.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 만료된 대화 세션을 주기적으로 정리하기 위해 스케줄링을 켠다.
 */
@Configuration
@EnableScheduling
class SchedulingConfiguration
