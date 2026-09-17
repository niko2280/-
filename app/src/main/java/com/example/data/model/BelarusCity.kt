package com.example.data.model

data class BelarusCity(
    val id: String,
    val name: String,
    val nameEn: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val isCapital: Boolean = false,
    val isOblastCenter: Boolean = false
) {
    companion object {
        val ALL = listOf(
            BelarusCity("minsk", "Минск", "Minsk", "Минская область", 53.9045, 27.5615, isCapital = true, isOblastCenter = true),
            BelarusCity("brest", "Брест", "Brest", "Брестская область", 52.0976, 23.7341, isOblastCenter = true),
            BelarusCity("grodno", "Гродно", "Grodno", "Гродненская область", 53.6694, 23.8131, isOblastCenter = true),
            BelarusCity("vitebsk", "Витебск", "Vitebsk", "Витебская область", 55.1904, 30.2049, isOblastCenter = true),
            BelarusCity("mogilev", "Могилёв", "Mogilev", "Могилёвская область", 53.9007, 30.3313, isOblastCenter = true),
            BelarusCity("gomel", "Гомель", "Gomel", "Гомельская область", 52.4345, 30.9754, isOblastCenter = true),
            BelarusCity("bobruisk", "Бобруйск", "Bobruisk", "Могилёвская область", 53.1384, 29.2214),
            BelarusCity("baranovichi", "Барановичи", "Baranovichi", "Брестская область", 53.1327, 26.0139),
            BelarusCity("borisov", "Борисов", "Borisov", "Минская область", 54.2276, 28.5052),
            BelarusCity("pinsk", "Пинск", "Pinsk", "Брестская область", 52.1153, 26.0954),
            BelarusCity("orsha", "Орша", "Orsha", "Витебская область", 54.5078, 30.4287),
            BelarusCity("mozyr", "Мозырь", "Mozyr", "Гомельская область", 52.0495, 29.2456),
            BelarusCity("soligorsk", "Солигорск", "Soligorsk", "Минская область", 52.7876, 27.5415),
            BelarusCity("novopolotsk", "Новополоцк", "Novopolotsk", "Витебская область", 55.5324, 28.6534),
            BelarusCity("polotsk", "Полоцк", "Polotsk", "Витебская область", 55.4856, 28.7686),
            BelarusCity("lida", "Лида", "Lida", "Гродненская область", 53.8833, 25.2997),
            BelarusCity("molodechno", "Молодечно", "Molodechno", "Минская область", 54.3167, 26.8500),
            BelarusCity("zhlobin", "Жлобин", "Zhlobin", "Гомельская область", 52.8929, 30.0333),
            BelarusCity("svetlogorsk", "Светлогорск", "Svetlogorsk", "Гомельская область", 52.6333, 29.7333),
            BelarusCity("rechitsa", "Речица", "Rechitsa", "Гомельская область", 52.3639, 30.3917),
            BelarusCity("slutsk", "Слуцк", "Slutsk", "Минская область", 53.0274, 27.5598),
            BelarusCity("braslav", "Браслав", "Braslav", "Витебская область", 55.6372, 27.0422),
            BelarusCity("nesvizh", "Несвиж", "Nesvizh", "Минская область", 53.2186, 26.6775),
            BelarusCity("kobrin", "Кобрин", "Kobrin", "Брестская область", 52.2139, 24.3564),
            BelarusCity("volkovysk", "Волковыск", "Volkovysk", "Гродненская область", 53.1606, 24.4533)
        )

        val DEFAULT = ALL[0] // Минск
    }
}
