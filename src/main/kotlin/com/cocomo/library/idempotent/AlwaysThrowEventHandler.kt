package com.cocomo.library.idempotent

import org.springframework.util.ErrorHandler

class AlwaysThrowErrorHandler : ErrorHandler {
    override fun handleError(t: Throwable) = throw t
}