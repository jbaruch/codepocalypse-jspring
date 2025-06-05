package com.airline.loyalty

import dev.langchain4j.data.message.UserMessage
import io.quarkiverse.langchain4j.guardrails.InputGuardrail
import io.quarkiverse.langchain4j.guardrails.InputGuardrailResult

internal class LoyaltyGuardrails : InputGuardrail {
    override fun validate(userMessage: UserMessage): InputGuardrailResult? {
        return if (userMessage.contents().contains<Any>("RyanAir")) {
            failure("We don't talk about those guys!")
        } else {
            success()
        }
    }
}