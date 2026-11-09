package com.tmc.mobile.domain.port

class ServerUnreachableException : Exception("Server unreachable")

class InvalidCredentialsException : Exception("Invalid employee ID or password")

class SessionExpiredException : Exception("No valid session")
