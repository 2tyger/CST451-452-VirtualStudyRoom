/*
contains frontend service tests for task api wiring
*/
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TaskApiService } from './task-api.service';
import { environment } from '../../../environments/environment';

describe('TaskApiService', () => {
  let service: TaskApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(TaskApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should list tasks', () => {
    service.listTasks(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10/tasks`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should create task', () => {
    service.createTask(10, 'Write tests', 'coverage').subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10/tasks`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Write tests', description: 'coverage' });
    req.flush({});
  });

  it('should update task with patch payload', () => {
    service.updateTask(10, 5, { done: true }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10/tasks/5`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ done: true });
    req.flush({});
  });

  it('should delete task', () => {
    service.deleteTask(10, 5).subscribe();

    const req = httpMock.expectOne(`${environment.apiBase}/rooms/10/tasks/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
