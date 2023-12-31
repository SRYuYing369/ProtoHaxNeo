package dev.sora.relay.session.listener.xbox

import coelho.msftauth.api.xbox.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nimbusds.jose.Payload
import com.nimbusds.jwt.SignedJWT
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerEncryptedSession
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.base64Decode
import dev.sora.relay.utils.logError
import dev.sora.relay.utils.logInfo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import java.io.Reader
import java.security.KeyPair
import java.security.PublicKey
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class RelayListenerXboxLogin(val accessToken: String, val deviceInfo: XboxDeviceInfo) : RelayListenerEncryptedSession() {

    constructor(accessToken: String, deviceInfo: XboxDeviceInfo, session: MinecraftRelaySession) : this(accessToken, deviceInfo) {
        this.session = session
    }

    private var chainExpires = 0L
    private var identityToken = ""
        get() {
            if (field.isEmpty()) {
                field = fetchIdentityToken(accessToken, deviceInfo)
            }
            return field
        }
    private var chain: List<SignedJWT>? = null
        get() {
            if (field == null || chainExpires < Instant.now().epochSecond) {
                val chains = fetchChain(identityToken, keyPair)
				field = chains

				// search for chain expiry
				chainExpires = 0L
				chains.forEach {  chain ->
					val expires = chain.payload.toJSONObject()["exp"] ?: return@forEach
					if (expires is Number && (chainExpires == 0L || expires.toLong() < chainExpires)) {
						chainExpires = expires.toLong()
					}
				}
            }
            return field
        }

    fun forceFetchChain() {
        chain
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is LoginPacket) {
            try {
                packet.chain.clear()
                packet.chain.addAll(chain!!)
				packet.extra = signJWT(packet.extra.payload, keyPair)
            } catch (e: Throwable) {
                session.inboundPacket(DisconnectPacket().apply {
                    kickMessage = e.toString()
                })
                logError("login failed", e)
            }
            logInfo("login success")
        }

        return true
    }


    companion object {

		/**
		 * this key used to sign the post content
		 */
		val deviceKey = XboxDeviceKey()

        fun fetchIdentityToken(accessToken: String, deviceInfo: XboxDeviceInfo): String {
            var userToken: XboxToken? = null
            val userRequestThread = thread {
                userToken = XboxUserAuthRequest(
                    "http://auth.xboxlive.com", "JWT", "RPS",
                    "user.auth.xboxlive.com", "t=$accessToken"
                ).request(HttpUtils.client)
            }
            val deviceToken = XboxDeviceAuthRequest(
                "http://auth.xboxlive.com", "JWT", deviceInfo.deviceType,
                "0.0.0.0", deviceKey
            ).request(HttpUtils.client)
            val titleToken = if (deviceInfo.allowDirectTitleTokenFetch) {
                XboxTitleAuthRequest(
                    "http://auth.xboxlive.com", "JWT", "RPS",
                    "user.auth.xboxlive.com", "t=$accessToken", deviceToken.token, deviceKey
                ).request(HttpUtils.client)
            } else {
                val device = XboxDevice(deviceKey, deviceToken)
				val sisuQuery = XboxSISUAuthenticateRequest.Query("phone")
                val sisuRequest = XboxSISUAuthenticateRequest(
                    deviceInfo.appId, device, "service::user.auth.xboxlive.com::MBI_SSL",
                    sisuQuery, deviceInfo.xalRedirect, "RETAIL"
                ).request(HttpUtils.client)
                val sisuToken = XboxSISUAuthorizeRequest(
					"t=$accessToken", deviceInfo.appId, device, "RETAIL",
					sisuRequest.sessionId, "user.auth.xboxlive.com", "http://xboxlive.com"
				).request(HttpUtils.client)
				if (sisuToken.status != 200) {
					val did = deviceToken.displayClaims["xdi"]!!.asJsonObject.get("did").asString
					val sign = deviceKey.sign("/proxy?sessionid=${sisuRequest.sessionId}", null, "POST", null).replace("+", "%2B").replace("=", "%3D")
					val url = sisuToken.webPage.split("#")[0] +
						"&did=0x$did&redirect=${deviceInfo.xalRedirect}" +
						"&sid=${sisuRequest.sessionId}&sig=${sign}&state=${sisuQuery.state}"
					throw XboxGamerTagException(url)
				}
                sisuToken.titleToken
            }
            if (userRequestThread.isAlive)
                userRequestThread.join()
            if (userToken == null) error("failed to fetch xbox user token")
            val xstsToken = XboxXSTSAuthRequest(
                "https://multiplayer.minecraft.net/",
                "JWT",
                "RETAIL",
                listOf(userToken),
                titleToken,
                XboxDevice(deviceKey, deviceToken)
            ).request(HttpUtils.client)

            return xstsToken.toIdentityToken()
        }

        fun fetchRawChain(identityToken: String, publicKey: PublicKey): Reader {
            // then, we can request the chain
            val data = JsonObject().apply {
                addProperty("identityPublicKey", Base64.getEncoder().withoutPadding().encodeToString(publicKey.encoded))
            }
			val request = Request.Builder()
				.url("https://multiplayer.minecraft.net/authentication")
				.post(AbstractConfigManager.DEFAULT_GSON.toJson(data).toRequestBody("application/json".toMediaType()))
				.header("Client-Version", "1.19.50")
				.header("Authorization", identityToken)
				.build()
			val response = HttpUtils.client.newCall(request).execute()

			assert(response.code == 200) { "Http code ${response.code}" }

			return response.body!!.charStream()
        }

        fun fetchChain(identityToken: String, keyPair: KeyPair): List<SignedJWT> {
            val rawChain = JsonParser.parseReader(fetchRawChain(identityToken, keyPair.public)).asJsonObject
            val chains = rawChain.get("chain").asJsonArray

            // add the self-signed jwt
            val identityPubKey = JsonParser.parseString(base64Decode(chains.get(0).asString.split(".")[0]).toString(Charsets.UTF_8)).asJsonObject

            val jwt = signJWT(Payload(AbstractConfigManager.DEFAULT_GSON.toJson(JsonObject().apply {
				addProperty("certificateAuthority", true)
				addProperty("exp", (Instant.now().epochSecond + TimeUnit.HOURS.toSeconds(6)).toInt())
				addProperty("nbf", (Instant.now().epochSecond - TimeUnit.HOURS.toSeconds(6)).toInt())
				addProperty("identityPublicKey", identityPubKey.get("x5u").asString)
			})), keyPair)

            val list = mutableListOf(jwt)
			list.addAll(chains.map { SignedJWT.parse(it.asString) })
            return list
        }
    }
}
