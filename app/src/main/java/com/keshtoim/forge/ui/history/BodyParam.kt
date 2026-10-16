package com.keshtoim.forge.ui.history

import androidx.annotation.StringRes
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.BodyMeasurement

// Everything a body measurement can hold, in the order the log and the editor show it.
enum class BodyParam(
    @StringRes val labelRes: Int,
    @StringRes val unitRes: Int,
    val range: ClosedFloatingPointRange<Double>,
    val get: (BodyMeasurement) -> Double?,
    val set: (BodyMeasurement, Double?) -> BodyMeasurement,
) {
    WEIGHT(R.string.profile_weight, R.string.unit_kg, 20.0..400.0, { it.weightKg }, { m, v -> m.copy(weightKg = v) }),
    NECK(R.string.body_neck, R.string.unit_cm, 15.0..80.0, { it.neckCm }, { m, v -> m.copy(neckCm = v) }),
    CHEST(R.string.body_chest, R.string.unit_cm, 40.0..200.0, { it.chestCm }, { m, v -> m.copy(chestCm = v) }),
    WAIST(R.string.body_waist, R.string.unit_cm, 30.0..250.0, { it.waistCm }, { m, v -> m.copy(waistCm = v) }),
    HIPS(R.string.body_hips, R.string.unit_cm, 40.0..250.0, { it.hipsCm }, { m, v -> m.copy(hipsCm = v) }),
    BICEPS(R.string.body_biceps, R.string.unit_cm, 10.0..80.0, { it.bicepsCm }, { m, v -> m.copy(bicepsCm = v) }),
    THIGH(R.string.body_thigh, R.string.unit_cm, 20.0..120.0, { it.thighCm }, { m, v -> m.copy(thighCm = v) }),
    CALF(R.string.body_calf, R.string.unit_cm, 15.0..80.0, { it.calfCm }, { m, v -> m.copy(calfCm = v) }),
}
