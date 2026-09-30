import {afterEach,describe,it,expect,vi} from 'vitest';
import {render,screen,fireEvent,cleanup} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {AnalysisProvider} from './AnalysisProvider';
import {AnalyticsPanel} from './AnalyticsPanel';
import {sampleMessages} from '../../mocks/chatApi';
import {mockSnapshot} from '../../mocks/analysisApi';
const messages=sampleMessages();
vi.mock('../chat/ChatProvider',()=>({useChat:()=>({messages,loading:false})}));
afterEach(cleanup);
describe('analysis adapter failures',()=> {
  it('shows an actual adapter error and retries into a labeled mock result',async()=> {
    const api={getConversation:vi.fn().mockRejectedValueOnce(new Error('Fixture service unavailable')).mockResolvedValue(mockSnapshot(messages))};
    render(<MemoryRouter><AnalysisProvider api={api}><AnalyticsPanel/></AnalysisProvider></MemoryRouter>);
    expect(await screen.findByRole('alert')).toHaveTextContent('Fixture service unavailable');
    fireEvent.click(screen.getByRole('button',{name:'Try again'}));
    expect(await screen.findByText('Mock fixtures')).toBeVisible();expect(screen.getAllByRole('progressbar')).toHaveLength(12);expect(api.getConversation).toHaveBeenCalledTimes(2);
  });
});
