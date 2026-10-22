import Link from 'next/link';
import {PageIntro} from '../../components/shell';
export const metadata={title:'Обновления — Space Client'};
export default function Updates(){return <div className="container page-content"><PageIntro label="ЗАМЕТКИ С ОРБИТЫ" title="Шаг за шагом.">Реальные изменения проекта. Заметки разработки ещё не означают выпуск клиента.</PageIntro><div className="article-links"><Link className="page-panel" href="/updates/first-contact"><p className="eyebrow">РАЗРАБОТКА · <time dateTime="2026-10-23">23 октября 2026</time></p><h2>Начинаем с фундамента.</h2><p>Первый Java-мод, исправление логирования и ТЗ нового сайта.</p><span className="text-link">Читать заметку →</span></Link></div></div>;}
