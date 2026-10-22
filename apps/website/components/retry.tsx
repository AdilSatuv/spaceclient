'use client';
import { useRouter } from 'next/navigation';
import { useTransition } from 'react';
export default function Retry(){const router=useRouter();const [pending,start]=useTransition();return <button className="button secondary" disabled={pending} onClick={()=>start(()=>router.refresh())}>{pending?'Проверяем каталог…':'Повторить запрос'}</button>;}
