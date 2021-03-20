package at.gpro.arbitrader.entity

interface Exchange {
    fun getName(): String
    fun getFee(): Double
    fun place(order: Order)
}