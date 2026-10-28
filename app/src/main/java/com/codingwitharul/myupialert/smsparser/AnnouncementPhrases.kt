package com.codingwitharul.myupialert.smsparser

import java.util.Locale

/**
 * Deterministic multi-language announcement templates for all 13 Indian languages.
 * Kotlin implementation for the open source edition.
 */
object AnnouncementPhrases {

    enum class Lang(val code: String, val label: String) {
        ENGLISH("en", "English"),
        HINDI("hi", "Hindi"),
        TAMIL("ta", "Tamil"),
        TELUGU("te", "Telugu"),
        KANNADA("kn", "Kannada"),
        MALAYALAM("ml", "Malayalam"),
        GUJARATI("gu", "Gujarati"),
        PUNJABI("pa", "Punjabi"),
        BENGALI("bn", "Bengali"),
        MARATHI("mr", "Marathi"),
        ODIA("or", "Odia"),
        ASSAMESE("as", "Assamese"),
        URDU("ur", "Urdu");

        companion object {
            @JvmStatic
            fun fromString(lang: String?): Lang {
                if (lang == null) return ENGLISH
                val lower = lang.lowercase(Locale.ROOT)
                return when {
                    lower.contains("hindi") -> HINDI
                    lower.contains("tamil") -> TAMIL
                    lower.contains("telugu") -> TELUGU
                    lower.contains("kannada") -> KANNADA
                    lower.contains("malayalam") -> MALAYALAM
                    lower.contains("gujarati") -> GUJARATI
                    lower.contains("punjabi") -> PUNJABI
                    lower.contains("bengali") -> BENGALI
                    lower.contains("marathi") -> MARATHI
                    lower.contains("odia") -> ODIA
                    lower.contains("assamese") -> ASSAMESE
                    lower.contains("urdu") -> URDU
                    else -> ENGLISH
                }
            }
        }
    }

    @JvmField
    val SUPPORTED_LANGUAGES = arrayOf(
        "English", "Hindi", "Tamil", "Telugu", "Kannada", "Malayalam",
        "Gujarati", "Punjabi", "Bengali", "Marathi", "Odia", "Assamese", "Urdu"
    )

    /**
     * Builds a natural, fluent spoken announcement phrase for the specified language.
     */
    @JvmStatic
    fun buildPhrase(languageStr: String?, amount: String?, paisa: String?, senderName: String?): String {
        val lang = Lang.fromString(languageStr)
        val hasPaisa = !paisa.isNullOrEmpty() && paisa != "0" && paisa != "00"
        val hasSender = !senderName.isNullOrBlank()
        val sender = if (hasSender) senderName!!.trim() else ""

        return when (lang) {
            Lang.HINDI -> {
                if (hasSender) {
                    if (hasPaisa) "$sender से $amount रुपये और $paisa पैसे प्राप्त हुए"
                    else "$sender से $amount रुपये प्राप्त हुए"
                } else {
                    if (hasPaisa) "$amount रुपये और $paisa पैसे प्राप्त हुए"
                    else "$amount रुपये प्राप्त हुए"
                }
            }
            Lang.TAMIL -> {
                if (hasSender) {
                    if (hasPaisa) "$sender இடமிருந்து $amount ரூபாய் மற்றும் $paisa காசுகள் பெறப்பட்டது"
                    else "$sender இடமிருந்து $amount ரூபாய் பெறப்பட்டது"
                } else {
                    if (hasPaisa) "$amount ரூபாய் மற்றும் $paisa காசுகள் பெறப்பட்டது"
                    else "$amount ரூபாய் பெறப்பட்டது"
                }
            }
            Lang.TELUGU -> {
                if (hasSender) {
                    if (hasPaisa) "$sender నుండి $amount రూపాయలు మరియు $paisa పైసలు అందాయి"
                    else "$sender నుండి $amount రూపాయలు అందాయి"
                } else {
                    if (hasPaisa) "$amount రూపాయలు మరియు $paisa పైసలు అందాయి"
                    else "$amount రూపాయలు అందాయి"
                }
            }
            Lang.KANNADA -> {
                if (hasSender) {
                    if (hasPaisa) "$sender ಅವರಿಂದ $amount ರೂಪಾಯಿ ಮತ್ತು $paisa ಪೈಸೆ ಜಮೆಯಾಗಿದೆ"
                    else "$sender ಅವರಿಂದ $amount ರೂಪಾಯಿ ಜಮೆಯಾಗಿದೆ"
                } else {
                    if (hasPaisa) "$amount ರೂಪಾಯಿ ಮತ್ತು $paisa ಪೈಸೆ ಜಮೆಯಾಗಿದೆ"
                    else "$amount ರೂಪಾಯಿ ಜಮೆಯಾಗಿದೆ"
                }
            }
            Lang.MALAYALAM -> {
                if (hasSender) {
                    if (hasPaisa) "$sender ൽ നിന്ന് $amount രൂപ $paisa പൈസ ലഭിച്ചു"
                    else "$sender ൽ നിന്ന് $amount രൂപ ലഭിച്ചു"
                } else {
                    if (hasPaisa) "$amount രൂപ $paisa പൈസ ലഭിച്ചു"
                    else "$amount രൂപ ലഭിച്ചു"
                }
            }
            Lang.GUJARATI -> {
                if (hasSender) {
                    if (hasPaisa) "$sender તરફથી $amount રૂપિયા અને $paisa પૈસા મળ્યા"
                    else "$sender તરફથી $amount રૂપિયા મળ્યા"
                } else {
                    if (hasPaisa) "$amount રૂપિયા અને $paisa પૈસા મળ્યા"
                    else "$amount રૂપિયા મળ્યા"
                }
            }
            Lang.PUNJABI -> {
                if (hasSender) {
                    if (hasPaisa) "$sender ਤੋਂ $amount ਰੁਪਏ ਅਤੇ $paisa ਪੈਸੇ ਪ੍ਰਾਪਤ ਹੋਏ"
                    else "$sender ਤੋਂ $amount ਰੁਪਏ ਪ੍ਰਾਪਤ ਹੋਏ"
                } else {
                    if (hasPaisa) "$amount ਰੁਪਏ ਅਤੇ $paisa ਪੈਸੇ ਪ੍ਰਾਪਤ ਹੋਏ"
                    else "$amount ਰੁਪਏ ਪ੍ਰਾਪਤ ਹੋਏ"
                }
            }
            Lang.BENGALI -> {
                if (hasSender) {
                    if (hasPaisa) "$sender থেকে $amount টাকা আর $paisa পয়সা পাওয়া গেছে"
                    else "$sender থেকে $amount টাকা পাওয়া গেছে"
                } else {
                    if (hasPaisa) "$amount টাকা আর $paisa পয়সা পাওয়া গেছে"
                    else "$amount টাকা পাওয়া গেছে"
                }
            }
            Lang.MARATHI -> {
                if (hasSender) {
                    if (hasPaisa) "$sender कडून $amount रुपये आणि $paisa पैसे मिळाले"
                    else "$sender कडून $amount रुपये मिळाले"
                } else {
                    if (hasPaisa) "$amount रुपये आणि $paisa पैसे मिळाले"
                    else "$amount रुपये मिळाले"
                }
            }
            Lang.ODIA -> {
                if (hasSender) {
                    if (hasPaisa) "$sender ଠାରୁ $amount ଟଙ୍କା ଏବଂ $paisa ପଇସା ମିଳିଲା"
                    else "$sender ଠାରୁ $amount ଟଙ୍କା ମିଳିଲା"
                } else {
                    if (hasPaisa) "$amount ଟଙ୍କା ଏବଂ $paisa ପଇସା ମିଳିଲା"
                    else "$amount ଟଙ୍କା ମିଳିଲା"
                }
            }
            Lang.ASSAMESE -> {
                if (hasSender) {
                    if (hasPaisa) "$sender ৰ পৰা $amount টকা আৰু $paisa পইচা পোৱা গল"
                    else "$sender ৰ পৰা $amount টকা পোৱা গল"
                } else {
                    if (hasPaisa) "$amount টকা আৰু $paisa পইচা পোৱা গল"
                    else "$amount টকা পোৱা গল"
                }
            }
            Lang.URDU -> {
                if (hasSender) {
                    if (hasPaisa) "$sender سے $amount روپے اور $paisa پیسے موصول ہوئے"
                    else "$sender سے $amount روپے موصول ہوئے"
                } else {
                    if (hasPaisa) "$amount روپے اور $paisa پیسے موصول ہوئے"
                    else "$amount روپے موصول ہوئے"
                }
            }
            Lang.ENGLISH -> {
                if (hasSender) {
                    if (hasPaisa) "Received $amount rupees and $paisa paisa from $sender"
                    else "Received $amount rupees from $sender"
                } else {
                    if (hasPaisa) "Received $amount rupees and $paisa paisa"
                    else "Received $amount rupees"
                }
            }
        }
    }

    @JvmStatic
    fun getUnknownAmountPhrase(languageStr: String?): String {
        return when (Lang.fromString(languageStr)) {
            Lang.HINDI -> "भुगतान प्राप्त हुआ"
            Lang.TAMIL -> "பணம் பெறப்பட்டது"
            Lang.TELUGU -> "చెల్లింపు అందింది"
            Lang.KANNADA -> "ಪಾವತಿ ಜಮೆಯಾಗಿದೆ"
            Lang.MALAYALAM -> "പേയ്‌മെന്റ് ലഭിച്ചു"
            Lang.GUJARATI -> "ચૂકવણી મળી"
            Lang.PUNJABI -> "ਭੁਗਤਾਨ ਪ੍ਰਾਪਤ ਹੋਇਆ"
            Lang.BENGALI -> "পেমেন্ট পাওয়া গেছে"
            Lang.MARATHI -> "पेमेंट मिळाले"
            Lang.ODIA -> "ପେମେଣ୍ଟ ମିଳିଲା"
            Lang.ASSAMESE -> "পেমেন্ট পোৱা গল"
            Lang.URDU -> "ادائیگی موصول ہوئی"
            Lang.ENGLISH -> "Payment received"
        }
    }
}
