package io.github.howshous.ui.util

fun defaultContractTitle(): String = "Rental Agreement"

fun defaultContractTerms(): String =
    """
    RENTAL AGREEMENT FOR BOARDING HOUSE

    TERMS AND CONDITIONS:

    1. RENTAL AMOUNT: [MONTHLY RENT] per month
    2. SECURITY DEPOSIT: [DEPOSIT] (refundable upon termination)
    3. PAYMENT TERMS: Monthly rent due on the 1st of each month
    4. DURATION: 12 months (renewable)
    5. UTILITIES: Included in rent (subject to fair usage policy)
    6. HOUSE RULES:
       - No smoking inside the premises
       - Quiet hours: 10 PM - 6 AM
       - Keep common areas clean
       - No pets without prior approval
    7. TERMINATION: 30 days written notice required
    8. DAMAGES: Tenant responsible for any damages beyond normal wear

    By signing this contract, both parties agree to the terms stated above.
    """.trimIndent()

fun defaultContractTermsFilled(monthlyRent: Int, deposit: Int): String {
    return defaultContractTerms()
        .replace("[MONTHLY RENT]", "PHP $monthlyRent")
        .replace("[DEPOSIT]", "PHP $deposit")
}
