package org.abendigo.plugin.csgo

import org.abendigo.csgo.*
import org.abendigo.csgo.Client.enemies
import org.abendigo.csgo.Engine.clientState
import org.abendigo.util.random
import org.abendigo.util.randomFloat
import org.jire.kotmem.Keys
import java.lang.Math.abs

object FOVAimPlugin : InGamePlugin(name = "Aimbot", duration = 16) {

	override val author = "Jire"
	override val description = "Aims at enemies when they are in LOCK_FOV"

	private const val AIM_KEY = 1 /* left click */
	private const val FORCE_AIM_KEY = 5 /* backwards button */
	private const val FORCE_AIM_ENHANCEMENT = 1.5F /* set to 1.0F for no enhancement */

	private const val LOCK_FOV = 40
	private const val UNLOCK_FOV = 70
	private const val NEVER_STICK = false

	private const val SMOOTHING_MIN = 5F
	private const val SMOOTHING_MAX = 8.25F

	private val TARGET_BONES = arrayOf(Bones.NECK, Bones.HEAD)
	private const val CHANGE_BONE_CHANCE = 8

	private var target: Player? = null
	private var targetBone = newTargetBone()

	private val aim = Vector(0F, 0F, 0F)

	override fun cycle() {
		val forceAim = Keys[FORCE_AIM_KEY]
		if (!forceAim && !Keys[AIM_KEY]) return

		val lockFOV = LOCK_FOV * FORCE_AIM_ENHANCEMENT
		val unlockFOV = UNLOCK_FOV * FORCE_AIM_ENHANCEMENT

		val smoothingMin = SMOOTHING_MIN * FORCE_AIM_ENHANCEMENT
		val smoothingMax = SMOOTHING_MAX * FORCE_AIM_ENHANCEMENT

		try {
			val weapon = +Me.weapon
			val weaponID = weapon.id
			if (weaponID == 42 || weaponID == 9 || weaponID == 40
					|| weaponID == 11 || weaponID == 38) return
		} catch (t: Throwable) {
		}

		val myPosition = +Me().position
		val angle = clientState(1024).angle()

		if (target == null) if (!findTarget(myPosition, angle, lockFOV)) return

		if (+Me().dead || +target!!.dead || !+target!!.spotted || +target!!.dormant) {
			target = null
			return
		}

		if (random(CHANGE_BONE_CHANCE) == 0) targetBone = newTargetBone()

		val enemyPosition = target!!.bonePosition(targetBone.id)

		val smoothing = randomFloat(smoothingMin, smoothingMax)

		compensateVelocity(Me(), target!!, enemyPosition, smoothing)

		calculateAngle(Me(), myPosition, enemyPosition, aim.reset())
		normalizeAngle(aim)

		normalizeAngle(angle)

		val distance = distance(myPosition, enemyPosition)
		val yawDelta = abs(angle.y - aim.y)
		val deltaFOV = abs(Math.sin(Math.toRadians(yawDelta.toDouble())) * distance)

		if (deltaFOV >= unlockFOV) target = null
		else angleSmooth(aim, angle, smoothing)

		if (NEVER_STICK) target = null
	}

	override fun disable() {
		target = null
	}

	private fun newTargetBone() = TARGET_BONES[random(TARGET_BONES.size)]

	private fun findTarget(myPosition: Vector<Float>, angle: Vector<Float>, lockFOV: Float): Boolean {
		var closestDelta = Int.MAX_VALUE
		var closetPlayer: Player? = null
		for ((i, e) in enemies) {
			if (+Me().dead) return false
			if (+e.dead || !+e.spotted || +e.dormant) continue

			val ePos = e.bonePosition(Bones.HEAD.id)
			val distance = distance(myPosition, ePos)

			calculateAngle(Me(), myPosition, ePos, aim.reset())
			normalizeAngle(aim)

			val yawDiff = abs(angle.y - aim.y)
			val delta = abs(Math.sin(Math.toRadians(yawDiff.toDouble())) * distance)

			if (delta <= lockFOV && delta < closestDelta) {
				closestDelta = delta.toInt()
				closetPlayer = e
			}
		}

		if (closetPlayer != null) {
			target = closetPlayer
			return true
		}

		return false
	}

}