package net.perfectdreams.dreamprivada

import com.okkero.skedule.schedule
import net.perfectdreams.dreamcore.utils.KotlinPlugin
import net.perfectdreams.dreamcore.utils.LocationUtils
import net.perfectdreams.dreamcore.utils.getStorageData
import net.perfectdreams.dreamcore.utils.lore
import net.perfectdreams.dreamcore.utils.registerEvents
import net.perfectdreams.dreamcore.utils.rename
import net.perfectdreams.dreamcore.utils.scheduler
import net.perfectdreams.dreamcore.utils.setStorageData
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class DreamPrivada : KotlinPlugin(), Listener {
	companion object {
		val NECESSIDADES_ITEM_KEY = UUID.fromString("687d1fcb-718c-43de-a8bc-ba8fc8c2ff4c")
	}

	val inToilet = Collections.newSetFromMap(WeakHashMap<Player, Boolean>())

	override fun softEnable() {
		super.softEnable()

		registerEvents(this)
	}

	@EventHandler
	fun onQuit(e: PlayerQuitEvent) {
		inToilet.remove(e.player)
	}

	@EventHandler
	fun onSneak(e: PlayerToggleSneakEvent) {
		if (!e.isSneaking)
			return

		val player = e.player

		if (isInAPrivada(player) && !inToilet.contains(player)) {
			player.sendMessage("§aFazendo necessidades...")
			inToilet.add(player)
			scheduler().schedule(this) {
				waitFor(1) // Isto é necessário já que player.isSneaking retornará false caso não esperar
				for (idx in 0..2) {
					if (!isInAPrivada(player) || !player.isSneaking) {
						inToilet.remove(player)
						player.sendMessage("§cSuas necessidades foram canceladas porque você se moveu!")
						player.spawnParticle(Particle.VILLAGER_ANGRY, player.location.add(0.0, 0.5, 0.0), 30, 0.5, 0.5, 0.5)
						return@schedule
					}
					player.spawnParticle(Particle.VILLAGER_ANGRY, player.location.add(0.0, 1.62, 0.0), 1)
					waitFor(20)
				}
				player.sendMessage("§aVocê se sente mais leve...")
				player.spawnParticle(Particle.VILLAGER_HAPPY, player.location.add(0.0, 0.5, 0.0), 30, 0.5, 0.5, 0.5)
				player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, 600, 1))
				inToilet.remove(player)

				val water = player.location.block.getRelative(BlockFace.DOWN)
				val necessidades = ItemStack(Material.INK_SACK, 1, 3.toShort())
						.rename("§8§lNecessidades")
						.lore("§7Se eu fosse você, eu não", "§7cheirava isto...", "§7", "§7Necessidades de §b${player.displayName}")
						.setStorageData("poop", NECESSIDADES_ITEM_KEY)

				player.world.dropItemNaturally(water.location.add(0.5, 0.5, 0.5), necessidades)
			}
		}
	}

	@EventHandler
	fun onButton(e: PlayerInteractEvent) {
		val button = e.clickedBlock
		val type = button?.type

		if (type == Material.STONE_BUTTON) {
			val face = when (button.data) {
				1.toByte() -> BlockFace.NORTH_WEST
				2.toByte() -> BlockFace.SOUTH_EAST
				3.toByte() -> BlockFace.NORTH_EAST
				4.toByte() -> BlockFace.SOUTH_WEST
				else -> BlockFace.UP
			}

			val trap = button.getRelative(face)

			if (trap.type == Material.TRAP_DOOR) {
				val water = trap.getRelative(BlockFace.DOWN)

				if (water.type == Material.STATIONARY_WATER && water.data == 0.toByte()) {
					e.isCancelled = true
					e.player.sendMessage("§7*sons de privada*")
					scheduler().schedule(this) {
						for (idx in 0..6) {
							water.data = idx.toByte()
							waitFor(5)
						}
						for (idx in 6 downTo 0) {
							water.data = idx.toByte()
							waitFor(5)
						}
					}
				}
			}
		}
	}

	@EventHandler
	fun onItemChange(e: PlayerItemHeldEvent) {
		val item = e.player.inventory.getItem(e.newSlot)

		if (item != null && item.type != Material.AIR) {
			val data = item.getStorageData(NECESSIDADES_ITEM_KEY)

			if (data != null) {
				e.player.removePotionEffect(PotionEffectType.CONFUSION)
				e.player.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 300, 0))
				e.player.sendMessage("§3Parece que isto não está cheirando bem...")
			}
		}
	}

	fun isInAPrivada(player: Player): Boolean {
		val block = player.location.block

		if (block.type == Material.TRAP_DOOR && block.getRelative(BlockFace.DOWN).type == Material.STATIONARY_WATER) {
			val face = LocationUtils.yawToFace((player.location.yaw + 90) % 360, true).oppositeFace

			if (block.getRelative(face).type == Material.QUARTZ_BLOCK)
				return true
		}
		return false
	}
}