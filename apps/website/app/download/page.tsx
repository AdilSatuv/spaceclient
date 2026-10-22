import Link from 'next/link';
import {PageIntro} from '../../components/shell';
import Retry from '../../components/retry';
export const metadata={title:'Загрузки — Space Client'};
export default async function Download(){
 let available=false;try{const response=await fetch(`${process.env.SPACE_API_URL||'http://127.0.0.1:3001'}/v1/releases`,{cache:'no-store',signal:AbortSignal.timeout(2000)});const data:unknown=await response.json();available=response.ok&&typeof data==='object'&&data!==null&&'items'in data&&Array.isArray(data.items)&&data.items.length===0;}catch{}
 return <div className="container page-content"><PageIntro label="ГОТОВНОСТЬ К СТАРТУ" title="До встречи на орбите.">Здесь появятся проверенные сборки Space Client и всё необходимое для установки.</PageIntro><div className="two-columns"><section className="page-panel"><span className="status">В РАЗРАБОТКЕ</span><h2 style={{marginTop:24}}>{available?'Первый релиз ещё впереди.':'Не удалось загрузить каталог.'}</h2><p>{available?'Пользовательский установщик пока не выпущен. Dev-мод не заменяет готовый клиент.':'Каталог сейчас недоступен. Это не означает, что релизов нет. Попробуйте обновить страницу позже.'}</p><div className="actions">{available?<Link className="button secondary" href="/updates">Следить за обновлениями</Link>:<Retry/>}<Link className="text-link" href="/help">Помощь →</Link></div></section><aside className="page-panel"><p className="eyebrow">ЦЕЛИ ПЕРВОЙ ВЕРСИИ</p><h2>Совместимость</h2><p>Minecraft Java 1.8.9<br/>Windows x64</p><p>Требования к памяти, диску и Java уточним перед alpha. Для macOS и Linux сборок пока нет.</p></aside></div></div>;
}

