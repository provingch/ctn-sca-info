import { api } from './client';

export interface PushSubscriptionStatus {
  publicKey: string;
  subscribed: boolean;
}

export interface PushSubscriptionPayload {
  endpoint: string;
  p256dh: string;
  auth: string;
}

export const getPushSubscriptionStatus = () => api.get<PushSubscriptionStatus>('/api/push-subscription');
export const savePushSubscription = (payload: PushSubscriptionPayload) => api.post<void>('/api/push-subscription/save', payload);
export const removePushSubscription = () => api.post<void>('/api/push-subscription/unsubscribe');
export const sendPushTest = () => api.post<void>('/api/push-subscription/test');

export function toPushPayload(subscription: PushSubscription): PushSubscriptionPayload {
  const json = subscription.toJSON();
  return {
    endpoint: subscription.endpoint,
    p256dh: json.keys?.p256dh ?? '',
    auth: json.keys?.auth ?? '',
  };
}

export function urlBase64ToUint8Array(value: string) {
  const padding = '='.repeat((4 - value.length % 4) % 4);
  const normalized = `${value}${padding}`.replace(/-/g, '+').replace(/_/g, '/');
  const decoded = window.atob(normalized);
  return Uint8Array.from(decoded, (character) => character.charCodeAt(0));
}
