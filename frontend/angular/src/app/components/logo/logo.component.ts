import { Component, Input } from '@angular/core';

/**
 * The Clientele logo: the mark (a heavy "C" with a person in its counter)
 * followed by the wordmark. `size` is the height of the mark in pixels; the
 * wordmark and gap scale with it in the proportions of the logo design.
 */
@Component({
  selector: 'app-logo',
  templateUrl: './logo.component.html',
  styleUrls: ['./logo.component.scss']
})
export class LogoComponent {
  @Input() size = 40;
  @Input() tone: 'color' | 'white' = 'color';

  get fontSize(): number {
    return Math.round(this.size * 0.81);
  }

  get gap(): number {
    return Math.round(this.size * 0.22);
  }
}
