package com.piloto.particles

/** Textos explicativos de las estructuras del corazón. */
object HeartInfo {
    val map: Map<String, Info> = listOf(
        "Aurícula derecha" to "Recibe la sangre pobre en oxígeno de todo el cuerpo a través de las venas cavas. En su pared se encuentra el nodo sinusal, el marcapasos natural. Al contraerse (sístole auricular) empuja la sangre por la válvula tricúspide hacia el ventrículo derecho; aporta cerca del 20 % del llenado ventricular.",
        "Ventrículo derecho" to "Bombea la sangre desoxigenada hacia los pulmones, a través de la válvula pulmonar y el tronco pulmonar. Su pared es más delgada que la del izquierdo porque la circulación pulmonar trabaja a baja presión (≈ 25/8 mmHg). Tiene forma de media luna y rodea al ventrículo izquierdo.",
        "Aurícula izquierda" to "Recibe la sangre recién oxigenada de los pulmones por las cuatro venas pulmonares y la envía al ventrículo izquierdo a través de la válvula mitral. Su orejuela es una zona donde pueden formarse coágulos cuando hay fibrilación auricular.",
        "Ventrículo izquierdo" to "Es la cámara más potente: su pared muscular de 8 a 12 mm impulsa la sangre a todo el cuerpo por la válvula aórtica, con una presión de ≈ 120 mmHg en la sístole. Expulsa unos 70 mL por latido (≈ 5 L por minuto en reposo). Al contraerse se acorta y se retuerce, como al escurrir un trapo.",
        "Vena cava superior" to "Recoge la sangre desoxigenada de la cabeza, el cuello, los brazos y la parte alta del tórax y la conduce a la aurícula derecha. No tiene válvulas: el flujo es a favor de la gravedad y de la succión torácica.",
        "Vena cava inferior" to "Es la vena más grande del cuerpo. Trae la sangre desoxigenada del abdomen y las piernas hasta la aurícula derecha, ayudada por la respiración y por el bombeo de los músculos de las piernas.",
        "Aorta" to "La arteria mayor del cuerpo. Nace en el ventrículo izquierdo, forma el arco aórtico (de él salen el tronco braquiocefálico, la carótida izquierda y la subclavia izquierda, que irrigan cabeza y brazos) y desciende repartiendo sangre oxigenada. Su elasticidad amortigua cada latido y mantiene el flujo durante la diástole.",
        "Aorta descendente" to "Continuación del arco aórtico: baja por delante de la columna, atraviesa el diafragma y reparte sangre oxigenada al tórax, al abdomen y a las piernas.",
        "Tronco pulmonar" to "Sale del ventrículo derecho y se divide en las arterias pulmonares derecha e izquierda, que llevan sangre pobre en oxígeno a los pulmones. Son las únicas arterias que transportan sangre desoxigenada.",
        "Venas pulmonares" to "Cuatro venas (dos por pulmón) que llevan sangre oxigenada a la aurícula izquierda. Son las únicas venas del cuerpo que transportan sangre rica en oxígeno.",
        "Válvula tricúspide" to "Tres valvas entre la aurícula y el ventrículo derechos. Se abre en la diástole y se cierra al comenzar la sístole ventricular; las cuerdas tendinosas y los músculos papilares impiden que se invierta hacia la aurícula.",
        "Válvula mitral" to "Dos valvas (bicúspide) entre la aurícula y el ventrículo izquierdos. Su cierre, junto al de la tricúspide, produce el primer ruido cardiaco (‘lub’) al empezar la sístole.",
        "Válvula pulmonar" to "Válvula semilunar de tres valvas a la salida del ventrículo derecho. Se abre durante la eyección y se cierra al final de la sístole, contribuyendo al segundo ruido cardiaco (‘dub’).",
        "Válvula aórtica" to "Tres valvas semilunares a la salida del ventrículo izquierdo. Justo detrás de ellas nacen las arterias coronarias que irrigan el propio corazón. Su cierre origina el segundo ruido cardiaco (‘dub’).",
        "Tabique interventricular" to "Pared muscular que separa ambos ventrículos e impide que se mezclen la sangre oxigenada y la desoxigenada. Por él discurren las ramas del sistema de conducción eléctrica.",
        "Esternón" to "Hueso plano en el centro del pecho que protege al corazón por delante. Queda justo delante del ventrículo derecho, la cámara más anterior.",
        "Columna vertebral" to "Está por detrás del corazón, junto a la aorta descendente y el esófago.",
        "Tráquea" to "Conducto de aire que baja por delante del esófago y se divide en los bronquios principales; queda por detrás de los grandes vasos.",
        "Nodo sinusal" to "Marcapasos natural, en la aurícula derecha. Genera de 60 a 100 impulsos por minuto y los propaga por las aurículas (onda P del ECG), que se contraen.",
        "Nodo auriculoventricular" to "Retrasa el impulso unos 0,1 s para que las aurículas terminen de vaciarse antes de que se contraigan los ventrículos (segmento PR del ECG).",
        "Haz de His y Purkinje" to "El impulso baja por el haz de His y sus ramas y se reparte por las fibras de Purkinje, activando los ventrículos de la punta hacia la base para una contracción sincronizada (complejo QRS).",
        "Arteria pulmonar" to "Ramas derecha e izquierda del tronco pulmonar: llevan la sangre desoxigenada a cada pulmón, donde se oxigena en los capilares alveolares."
    ).associate { (k, v) -> k to Info(k, v) }
}
