package org.partiql.iondb

class IonDbException(msg: String) : Exception(msg) {
    override fun fillInStackTrace(): Throwable {
        return this
    }
}
