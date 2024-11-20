package org.partiql.iondb

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.Severity

object IonErrorListener : PErrorListener {
    override fun report(error: PError) {
        val message = ErrorMessageFormatter.message(error)
        if (error.severity.code() == Severity.ERROR) {
            throw IonDbException(message)
        }
        else {
            println(message)
        }
    }
}
