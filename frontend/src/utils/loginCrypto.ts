import JSEncrypt from 'jsencrypt'
import { request } from '@/api/request'

interface PublicKeyVO {
  publicKey: string
}

/**
 * 登录密码加密：先向后端获取 RSA 公钥，再用公钥加密密码。
 *
 * 每次登录都重新取公钥，避免后端重启后仍使用旧公钥导致解密失败。
 * 加密失败时抛出异常，绝不降级为明文发送。
 */
export async function encryptPassword(password: string): Promise<string> {
  const { publicKey } = await request<PublicKeyVO>({
    url: '/auth/public-key',
    method: 'GET'
  })
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKey)
  const encrypted = encryptor.encrypt(password)
  if (!encrypted) {
    throw new Error('密码加密失败')
  }
  return encrypted
}
