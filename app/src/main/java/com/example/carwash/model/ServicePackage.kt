package com.example.carwash.model


enum class VehicleSize{
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}



data class ServicePackage (
    val id: String,
    val name: String,
    val price: Double = 0.0,
    val description: String,
    val prices: Map<VehicleSize, Double>,
    val customPriceAllowed: Boolean = false

) {
    fun getPrice(vehicleSize: VehicleSize?): Double {
        return prices[vehicleSize] ?: price
    }

    fun getPriceForLabel(sizeLabel: String?): Double {
        if (sizeLabel.isNullOrBlank()) return price
        val foundEnum = VehicleSize.entries.find { it.name.equals(sizeLabel, ignoreCase = true) }
        if (foundEnum != null && prices.containsKey(foundEnum)) {
            return prices[foundEnum] ?: price
        }
        return price
    }

    companion object {

        val defaultPackages = listOf(

            ServicePackage(
            id = "package 4",
            name = "Package 4",
            description = "Carwash + Vacuum + ArmorAll + Wax + BackToZero",
            prices = mapOf(
                VehicleSize.SMALL to 250.0,
                VehicleSize.MEDIUM to 300.0,
                VehicleSize.LARGE to 350.0,
                VehicleSize.EXTRA_LARGE to 400.0
            )
        ),
            ServicePackage (
                id = "Package 1",
                name = "Package 1",
                description = "Carwash + Vacuum",
                prices = mapOf(
                    VehicleSize.SMALL to 150.0,
                    VehicleSize.MEDIUM to 200.0,
                    VehicleSize.LARGE to 230.0,
                    VehicleSize.EXTRA_LARGE to 250.0
                )
            ),
                ServicePackage (
                    id = "Package 2",
                    name = "Package 2",
                    description = "Carwash + Vacuum + ArmorAll",
                    prices = mapOf(
                        VehicleSize.SMALL to 210.0,
                        VehicleSize.MEDIUM to 260.0,
                        VehicleSize.LARGE to 280.0,
                        VehicleSize.EXTRA_LARGE to 300.0
                    )
                    ),
            ServicePackage (
                id = "Package 3",
                name = "Package 3",
                description = "Carwash + Vacuum + Wax",
                prices = mapOf(
                    VehicleSize.SMALL to 220.0,
                    VehicleSize.MEDIUM to 270.0,
                    VehicleSize.LARGE to 300.0,
                    VehicleSize.EXTRA_LARGE to 350.0
                )
                ),

            ServicePackage (
                id = "Package 5",
                name = "Package 5",
                description = "Carwash +ArmorAll + Wax + CarpetShampoo + BackToZero",
                prices = mapOf(
                    VehicleSize.SMALL to 500.0,
                    VehicleSize.MEDIUM to 550.0,
                    VehicleSize.LARGE to 600.0,
                    VehicleSize.EXTRA_LARGE to 700.0
                )
            ),
            ServicePackage (
                id = "Package 6",
                name = "Package 6",
                description = "Carwash +ArmorAll + Wax + EngineWash + BackToZero",
                prices = mapOf(
                    VehicleSize.SMALL to 500.0,
                    VehicleSize.MEDIUM to 550.0,
                    VehicleSize.LARGE to 600.0,
                    VehicleSize.EXTRA_LARGE to 650.0
                )
            ),
            ServicePackage (
                id = "Motor",
                name = "Motor",
                description = "Motor Wax",
                prices = mapOf(
                    VehicleSize.SMALL to 100.0,
                    VehicleSize.MEDIUM to 150.0,
                    VehicleSize.LARGE to 180.0,
                    VehicleSize.EXTRA_LARGE to 200.0
                )
            ),
            ServicePackage(
                id = "others",
                name = "Others",
                description = "Custom Service",
                prices = emptyMap(),
                customPriceAllowed = true,
            
            )
        )
    }
}