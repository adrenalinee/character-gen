package ltmp.charactergen.service.text

/**
 * 두 한국어 문장이 같은 사건을 말하는지 어림잡기 위한 명사 추출.
 * (L0 캐릭터 생성 프롬프트 v4.3 P-10, 런타임 프롬프트 조립 스펙 v1.1 Q-3)
 *
 * 형태소 분석기를 붙이지 않고 휴리스틱으로 간다. 이 판정의 쓰임은 두 곳뿐이고 둘 다 값을 고치지 않는
 * 경고·백스톱이라, 분석기 의존성과 사전 관리 비용을 새로 떠안을 근거가 없다.
 *
 * 대신 오탐과 미탐이 모두 있다. 조사만 벗기므로 용언의 활용형이 명사처럼 남고, 복합명사는 쪼개지지 않는다.
 * 임계값 [NOUN_OVERLAP_THRESHOLD]를 3으로 둔 것은 그 오차를 감안한 것이다.
 */

/** 같은 사건을 말하고 있다고 볼 명사 교집합 개수. (v4.3 P-10, 스펙 v1.1 Q-3) */
const val NOUN_OVERLAP_THRESHOLD = 3

/**
 * 한글 이외 문자로 끊은 토큰에서 조사 꼬리를 벗기고, 두 글자 이상인 것만 남긴다.
 *
 * 한 글자를 버리는 것은 조사를 벗기고 남은 찌꺼기가 대부분 한 글자이기 때문이다.
 * [KOREAN_STOP_WORDS]는 어느 캐릭터 문장에나 나와서 교집합을 부풀리는 말이다.
 */
fun koreanNouns(text: String): Set<String> =
    NON_HANGUL_REGEX.split(text)
        .map { stripParticle(it) }
        .filterTo(mutableSetOf()) { it.length >= MIN_NOUN_LENGTH && it !in KOREAN_STOP_WORDS }

/**
 * 두 문장이 같은 사건을 말하는지. 명사 교집합이 [NOUN_OVERLAP_THRESHOLD]개 이상이면 그렇게 본다.
 */
fun sharedKoreanNouns(left: String, right: String): Set<String> =
    koreanNouns(left) intersect koreanNouns(right)

/**
 * 긴 조사부터 벗긴다. 짧은 것을 먼저 보면 '에서'의 '서'나 '으로'의 '로'가 잘못 떨어진다.
 * 벗기고 남는 것이 [MIN_NOUN_LENGTH]보다 짧으면 조사가 아니라 낱말의 일부로 보고 그대로 둔다.
 */
private fun stripParticle(token: String): String {
    val particle = PARTICLES.firstOrNull {
        token.endsWith(it) && token.length - it.length >= MIN_NOUN_LENGTH
    } ?: return token
    return token.dropLast(particle.length)
}

private const val MIN_NOUN_LENGTH = 2

private val NON_HANGUL_REGEX = Regex("[^가-힣]+")

/** 길이 내림차순으로 둔다. [stripParticle]이 앞에서부터 훑는다. */
private val PARTICLES = listOf(
    "에게서", "한테서",
    "에서", "으로", "에게", "한테", "까지", "부터", "보다", "처럼", "마저", "조차", "이나",
    "은", "는", "이", "가", "을", "를", "의", "에", "로", "와", "과", "도", "만", "라", "야",
)

/**
 * 어느 캐릭터 문장에나 나오는 말. 남겨 두면 서로 다른 사건끼리도 교집합이 임계값에 닿는다.
 */
private val KOREAN_STOP_WORDS = setOf(
    "그것", "이것", "저것", "그때", "이때", "자신", "사람", "사람들", "때문", "동안", "정도",
    "경우", "다시", "그런", "이런", "저런", "무엇", "누구", "어떤", "가장", "여기", "거기",
    "하지", "하고", "않고", "않는", "있는", "없는", "되는", "같은", "많은", "모든",
)
